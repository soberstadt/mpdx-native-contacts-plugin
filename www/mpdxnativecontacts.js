var exec = require('cordova/exec');

exports.addAccount = function(name, accessToken, success, error) {
  exec(success, error, "MPDXNativeContacts", "addAccount", [name, accessToken]);
};
exports.removeAccounts = function(success, error) {
  exec(success, error, "MPDXNativeContacts", "removeAccounts", []);
};
exports.startSync = function(success, error) {
  exec(success, error, "MPDXNativeContacts", "startSync", []);
};
