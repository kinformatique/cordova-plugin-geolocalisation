// Module global Cordova
module.exports = {

    demarrer: function (parametrage, utilisateur, cleApi, version, url) {
        cordova.exec(null, null, "GeolocalisationPlugin", "demarrer", [parametrage, utilisateur, cleApi, version, url]);
    },

    arreter: function () {
        cordova.exec(null, null, "GeolocalisationPlugin", "arreter", []);
    }

};
