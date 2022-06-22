package com.example.plugin;

import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import android.content.Intent;
import androidx.core.content.ContextCompat;

import com.example.plugin.GeolocalisationService;
import android.content.Context;
import android.content.pm.PackageManager;


public class GeolocalisationPlugin extends CordovaPlugin {

    JSONArray data;
    Context context;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        this.context = this.cordova.getActivity().getApplicationContext();
        this.data = data;

        if (action.equals("demarrer")) {
            if(!this.permissionsAccordees()){
                this.demanderPermissions();
            } else {
                this.demarrerService();
            }

            callbackContext.success("Geolocalisation demarree");
            return true;
        } else if (action.equals("arreter")) {
            Intent serviceIntent = new Intent(this.context, GeolocalisationService.class);
          //  stopService(serviceIntent);
            callbackContext.success("Geolocalisation arretee");
            return true;
        } else {
            return false;
        }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean permissionsOK = true;
        for(int r:grantResults){
            if(r == PackageManager.PERMISSION_DENIED){
                permissionsOK = false;
            }
        }

        if(permissionsOK){
            this.demarrerService();
        }
    }

    private void demarrerService() {
        try {
            Intent serviceIntent = new Intent(this.context, GeolocalisationService.class);
            serviceIntent.putExtra("parametrage", this.data.getString(0));
            serviceIntent.putExtra("utilisateur", this.data.getString(1));
            serviceIntent.putExtra("cleApi", this.data.getString(2));
            serviceIntent.putExtra("version", this.data.getString(3));
            serviceIntent.putExtra("url", this.data.getString(4));
            ContextCompat.startForegroundService(this.context, serviceIntent);
        } catch (JSONException e){

        }
    }

    private boolean permissionsAccordees(){
        return cordova.hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                && cordova.hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                && cordova.hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                && cordova.hasPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private void demanderPermissions(){
        String [] permissions = {
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
        };

        cordova.requestPermissions(this, 1, permissions);
    }

}
