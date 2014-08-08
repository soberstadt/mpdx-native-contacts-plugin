var exec = require('cordova/exec');

exports.addAccount = function(name, accessToken, success, error) {
  exec(success, error, "NativeContactsPlugin", "addAccount", [name, accessToken]);
};
exports.removeAccount = function(success, error) {
  exec(success, error, "NativeContactsPlugin", "removeAccount", []);
};
exports.startSync = function(success, error) {
  exec(success, error, "NativeContactsPlugin", "startSync", []);
};
