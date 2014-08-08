package org.mpdx.nativecontacts;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class NativeContactsPlugin extends CordovaPlugin {

    public static final String ACTION_ADD_ACCOUNT = "addAccount";
    public static final String ACTION_START_SYNC = "startSync";
    public static final String ACTION_REMOVE_ACCOUNT = "removeAccount";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(ACTION_ADD_ACCOUNT.equals(action)) {
            String name = args.getString(0);
            String accessToken = args.getString(1);
            this.addAccount(name, accessToken, callbackContext);
            return true;
        }
        if(ACTION_REMOVE_ACCOUNT.equals(action)) {
            this.removeAccount(callbackContext);
            return true;
        }
        if(ACTION_START_SYNC.equals(action)) {
            this.startSync(callbackContext);
            return true;
        }
        return false;
    }

    private void addAccount(String name, String accessToken, CallbackContext callbackContext) {
        if(name != null && name.length() > 0 && accessToken != null && accessToken.length() > 0) {
            callbackContext.success();
        } else {
            callbackContext.error("Expected two non-empty string arguments.");
        }
    }

    private void removeAccount(CallbackContext callbackContext) {
        callbackContext.success();
    }

    private void startSync(CallbackContext callbackContext) {
        callbackContext.success();
    }
}
