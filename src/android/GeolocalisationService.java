package com.example.plugin;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StrictMode;
import android.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/* Test Antoine pour Git */
public class GeolocalisationService extends Service implements LocationListener {

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    public static final String FICHIER_JSON = "/PositionsGPS.json";
    public static final String PARAMETRAGE_LE_PLUS_PRECIS = "P";
    public static final String PARAMETRAGE_ECONOMIE_ENERGIE = "E";
    public static final String PARAMETRAGE_AUCUN = "A";
    public static final String HEADER_UTILISATEUR = "kalico-user";
    public static final String HEADER_VERSION = "kalico-v";
    public static final String HEADER_CLE_API = "pdalivkey";

    private ConfigurationGeolocalisationService configurationGeolocalisationService = null;
    private LocationManager locationManager;


    // **** FONCTIONS OVERRIDE INUTILES ****

    @Override
    public void onCreate() { super.onCreate(); }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onDestroy() {
        this.locationManager.removeUpdates(this); // On arrête de regarder les changements de position
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    // *************************************


    // **** FONCTION DECLENCHEE QUAND ON DETECTE UNE NOUVELLE POSITION ****
    @Override
    public void onLocationChanged(@NonNull Location location) {
        this.envoyerPositionGPS(new EnregistrementPositionGPS(location.getLatitude(), location.getLongitude()));
    }

    // **** FONCTION DECLENCHEE AU DEMARRAGE DU SERVICE ****
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Autorisations de sécurité, sinon crash
        if (android.os.Build.VERSION.SDK_INT > 9) { 
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        String action = intent.getStringExtra("action");
        if (action.equals("demarrer")) {
            this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // On créé la notification
            String input = intent.getStringExtra("inputExtra");
            Intent notificationIntent = new Intent(this, GeolocalisationPlugin.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            this.createNotificationChannel();
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("KALICO LIV")
                    .setContentText(input)
                  //.setSmallIcon(R.drawable.ic_launcher) // Icone de barre de notification, compliqué à implémenter
                    .setContentIntent(pendingIntent)
                    .build();

            // On lance le service en arrière plan
            startForeground(1, notification);

            this.demarrerGeolocalisation(
                intent.getStringExtra("parametrage"),
                intent.getStringExtra("utilisateur"),
                intent.getStringExtra("cleApi"),
                intent.getStringExtra("version"),
                intent.getStringExtra("url")
            );

        } 
        else if (action.equals("arreter")) {
            this.arreterGeolocalisation();
        }

        return START_NOT_STICKY;
    }

    // **** FONCTION POUR PARAMETRER LA NOTIFICATION ****
    private void createNotificationChannel() {

        // On créé la notification qui apparaît quand le service tourne en arrière plan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);

        }

    }

    private void configurerGeolocalisation(String parametrage, String utilisateur, String cleApi, String version, String url) {
        switch (parametrage) {

            case PARAMETRAGE_LE_PLUS_PRECIS :
                this.configurationGeolocalisationService = new ConfigurationGeolocalisationService(
                        60000, // Une minute
                        200, // 200 mètres
                        utilisateur,
                        cleApi,
                        version,
                        url
                );
                break;

            case PARAMETRAGE_ECONOMIE_ENERGIE :
                this.configurationGeolocalisationService = new ConfigurationGeolocalisationService(
                        600000, // Dix minutes
                        10000, // 10 kilomètres
                        utilisateur,
                        cleApi,
                        version,
                        url
                );
                break;

            default:
                break;
        }
    }

    @SuppressLint("MissingPermission")
    private void demarrerGeolocalisation(String parametrage, String utilisateur, String cleApi, String version, String url) {

        this.configurerGeolocalisation(parametrage, utilisateur, cleApi, version, url);
        this.locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            this.configurationGeolocalisationService.intervalleTemps,
            this.configurationGeolocalisationService.intervalleDistance,
            this
        );

    }

    private void arreterGeolocalisation() {
        // On appelle l'envoi au cas où on n'avait pas réussi à re envoyé la dernière fois
        this.envoyerPositionGPS(null);
        stopForeground(true);
        stopSelf();
    }


    // **** FONCTIONS POUR LE TRAITEMENT DE LA GEOLOCALISATION ****
    private void ecrireFichierJson(String json) {

        try {
            File dossierDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File fichierJson = new File(dossierDownload.getAbsolutePath() + FICHIER_JSON);

            // Si le fichier n'existe pas, on le créé
            if (fichierJson == null) {
                fichierJson.createNewFile();
            }

            FileWriter writer = new FileWriter(fichierJson);
            writer.write(json);
            writer.close();
        }
        catch (IOException e) {}

    }

    private String lireFichierJson() {

        try {
            File dossierDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File fichierJson = new File(dossierDownload.getAbsolutePath() + FICHIER_JSON);

            if (fichierJson != null) {
                FileReader reader = new FileReader(fichierJson);
                char[] charBuffer = new char[(int) fichierJson.length()];
                reader.read(charBuffer);
                reader.close();
                return new String(charBuffer);
            }
            else {
                return "";
            }

        }
        catch (IOException e) { 
            return ""; 
        }
    }

    private void envoyerPositionGPS(EnregistrementPositionGPS enregistrementPositionGPS) {

        try {

            // On lit le fichier json pour récupérer les enregistrements précédents qui ne se sont pas envoyés
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<EnregistrementPositionGPS>>(){}.getType();
            List<EnregistrementPositionGPS> listeEnregistrementPositionGPS = gson.fromJson(this.lireFichierJson(), listType);
            if (listeEnregistrementPositionGPS == null) { 
                listeEnregistrementPositionGPS = new ArrayList<>(); 
            }

            if (enregistrementPositionGPS != null) { 
                listeEnregistrementPositionGPS.add(enregistrementPositionGPS); 
            }

            // On formatte les enregistrements en JSON puis en tableau de byte
            String jsonAEnvoyer = gson.toJson(listeEnregistrementPositionGPS);
            byte[] byteArray = jsonAEnvoyer.getBytes("utf-8");

            HttpsURLConnection connexion = this.creerConnexion();

            // On envoie
            try (OutputStream os = connexion.getOutputStream()) {
                os.write(byteArray);

                // Si on n'a pas d'erreur on vide le fichier, sinon on enregistre la nouvelle liste dedans
                int code = connexion.getResponseCode();
                os.close();

                if (code == 200) {
                    this.ecrireFichierJson("");
                } 
                else {
                    this.ecrireFichierJson(jsonAEnvoyer);
                }

            }
            catch (IOException e) { // On passe ici si on n'a pas de connexion
                this.ecrireFichierJson(jsonAEnvoyer);
            }

        }
        catch (IOException e) {}

    }

    private HttpsURLConnection creerConnexion() throws IOException {

        // On créé la connexion
        URL url = new URL(this.configurationGeolocalisationService.url);
        HttpsURLConnection connexion = (HttpsURLConnection) url.openConnection();
        connexion.setDoOutput(true);
        connexion.setRequestMethod("POST");

        // On créé le header
        connexion.setRequestProperty(HEADER_UTILISATEUR, this.configurationGeolocalisationService.utilisateur);
        connexion.setRequestProperty(HEADER_VERSION, this.configurationGeolocalisationService.version);
        connexion.setRequestProperty(HEADER_CLE_API, this.configurationGeolocalisationService.cleApi);
        connexion.setRequestProperty("Content-Type", "application/json");
        connexion.setRequestProperty("Accept", "application/json");
        connexion.setRequestProperty("Access-Control-Allow-Origin", "*");

        // On autorise les https avec un certificat invalide
        connexion.setHostnameVerifier(new HostnameVerifier() {

            @Override
            public boolean verify(String arg0, SSLSession arg1) { return true; }
            
        });

        return connexion;
    }

}


class EnregistrementPositionGPS implements Serializable {

    private long id;
    private String date;
    private double latitude;
    private double longitude;

    public EnregistrementPositionGPS(double latitude, double longitude) {
        Date dateEnregistrement = new Date();
        this.id = dateEnregistrement.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.date = sdf.format(dateEnregistrement);
        this.latitude = latitude;
        this.longitude = longitude;
    }

}


class ConfigurationGeolocalisationService {

    public int intervalleTemps;
    public int intervalleDistance;
    public String utilisateur;
    public String cleApi;
    public String version;
    public String url;

    public ConfigurationGeolocalisationService(int intervalleTemps, int intervalleDistance, String utilisateur, String cleApi, String version, String url) {
        this.intervalleTemps = intervalleTemps;
        this.intervalleDistance = intervalleDistance;
        this.utilisateur = utilisateur;
        this.cleApi = cleApi;
        this.version = version;
        this.url = url;
    }

}
