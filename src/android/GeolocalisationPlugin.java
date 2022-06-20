package com.example.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

public class GeolocalisationPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("demarrer")) {
            Intent serviceIntent = new Intent(this, GeolocalisationService.class);
            serviceIntent.putExtra("parametrage",data.getString(0));
            serviceIntent.putExtra("utilisateur",data.getString(1));
            serviceIntent.putExtra("cleApi",data.getString(2));
            serviceIntent.putExtra("version",data.getString(3));
            serviceIntent.putExtra("url", data.getString(4));
            ContextCompat.startForegroundService(this, serviceIntent);
            callbackContext.success("Geolocalisation demarree");
            return true;

        } else if (action.equals("arreter")) {
            Intent serviceIntent = new Intent(this, GeolocalisationService.class);
            stopService(serviceIntent);
            callbackContext.success("Geolocalisation arretee");
            return true;
        } else {
            callbackContext.failure("Geolocalisation erreur");
            return false;
        }
    }
}
