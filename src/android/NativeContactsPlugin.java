package org.mpdx.nativecontacts;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;

public class NativeContactsPlugin extends CordovaPlugin {

    public static final String ACCOUNT_TYPE = "org.mpdx";
    public static final String ACTION_ADD_ACCOUNT = "addAccount";
    public static final String ACTION_START_SYNC = "startSync";
    public static final String ACTION_REMOVE_ACCOUNT = "removeAccount";
    private AccountManager mManager;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(mManager == null)
            mManager = AccountManager.get(cordova.getActivity());

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

    private void addAccount(final String name, final String accessToken, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                if(name != null && name.length() > 0 && accessToken != null && accessToken.length() > 0) {
                    Account account = new Account(name, ACCOUNT_TYPE);
                    if(false == mManager.addAccountExplicitly(account, null, null)) {
                        callbackContext.error("Account with username already exists!");
                    }
                    mManager.setAuthToken(account, ACCOUNT_TYPE, accessToken);
                    callbackContext.success("Account added.");
                } else {
                    callbackContext.error("Expected two non-empty string arguments.");
                }
            }
        });
    }

    private void removeAccount(CallbackContext callbackContext) {
        callbackContext.success("Not implemented yet.");
    }

    private void startSync(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                Account[] accounts = mManager.getAccountsByType(ACCOUNT_TYPE);
                if(accounts.length == 0) {
                    callbackContext.error("Expected account to exist.");
                    return;
                }
                if(mManager.peekAuthToken(accounts[0], ACCOUNT_TYPE) == null) {
                    callbackContext.error("Expected account to have auth token.");
                    return;
                }
                Bundle b = new Bundle();
                b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                ContentResolver.requestSync(
                        accounts[0],
                        ACCOUNT_TYPE,
                        b);
                callbackContext.success("Sync started!");
            }
        });
    }
}
