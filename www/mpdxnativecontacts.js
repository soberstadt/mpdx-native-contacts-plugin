var exec = require('cordova/exec');

exports.addAccount = function(name, accessToken, success, error) {
  exec(success, error, "MPDXNativeContacts", "addAccount", [name, accessToken]);
};
exports.removeAccount = function(success, error) {
  exec(success, error, "MPDXNativeContacts", "removeAccount", []);
};
exports.startSync = function(success, error) {
  exec(success, error, "MPDXNativeContacts", "startSync", []);
};
