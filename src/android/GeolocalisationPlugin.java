package com.example.plugin;

import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import android.content.Intent;
import androidx.core.content.ContextCompat;

import java.util.logging.Logger;
import com.example.plugin.GeolocalisationService;
import android.content.Context;


public class GeolocalisationPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        Logger l = Logger.getLogger("GeolocalisationPlugin");

        l.info("******************************************************************************************************************* EXECUTE");

        Context context = this.cordova.getActivity().getApplicationContext();
        if (action.equals("demarrer")) {
            l.info("******************************************************************************************************************* DEMARRER");
            Intent serviceIntent = new Intent(context, GeolocalisationService.class);
            serviceIntent.putExtra("parametrage",data.getString(0));
            serviceIntent.putExtra("utilisateur",data.getString(1));
            serviceIntent.putExtra("cleApi",data.getString(2));
            serviceIntent.putExtra("version",data.getString(3));
            serviceIntent.putExtra("url", data.getString(4));
            ContextCompat.startForegroundService(context, serviceIntent);
            callbackContext.success("Geolocalisation demarree");
            return true;

        } else if (action.equals("arreter")) {
            Intent serviceIntent = new Intent(context, GeolocalisationService.class);
         //   stopService(serviceIntent);
            callbackContext.success("Geolocalisation arretee");
            return true;
        } else {
            return false;
        }
    }
}
