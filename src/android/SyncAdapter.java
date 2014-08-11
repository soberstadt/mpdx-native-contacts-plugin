package org.mpdx.nativecontacts;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.widget.Toast;

/**
 * Define a sync adapter for the app.
 *
 * <p>This class is instantiated in {@link SyncService}, which also binds SyncAdapter to the system.
 * SyncAdapter should only be initialized in SyncService, never anywhere else.
 *
 * <p>The system calls onPerformSync() via an RPC call through the IBinder object supplied by
 * SyncService.
 */
class SyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = "SyncAdapter";

    private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000;  // 15 seconds

    private static final int NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds

    /**
     * Content resolver, for performing database operations.
     */
    private final ContentResolver mContentResolver;
//  private final ContactAccessor mContactAccessor;

    /**
     * Project used when querying content provider. Returns all known fields.
     */
    private static final String[] PROJECTION = new String[] {
            ContactsContract.RawContacts.SOURCE_ID};

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
//        mContactAccessor = new ContactAccessor(context);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    @SuppressLint("NewApi")
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
//        mContactAccessor = new ContactAccessor(context);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Called by the Android system in response to a request to run the sync adapter. The work
     * required to read data from the network, parse it, and store it in the content provider is
     * done here. Extending AbstractThreadedSyncAdapter ensures that all methods within SyncAdapter
     * run on a background thread. For this reason, blocking I/O and other long-running tasks can be
     * run <em>in situ</em>, and you don't have to set up a separate thread for them.
     *
     * <p>This is where we actually perform any work required to perform a sync.
     * {@link AbstractThreadedSyncAdapter} guarantees that this will be called on a non-UI thread,
     * so it is safe to peform blocking I/O here.
     *
     * <p>The syncResult argument allows you to pass information back to the method that triggered
     * the sync.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "Beginning network synchronization");
        try {
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("access_token", "71bc66990725f07a843d8aafa7d5d59c"));
            qparams.add(new BasicNameValuePair("include", "people,phone_numbers,Contact.id,Contact.name,Person.first_name,Person.last_name,Person.id,Person.phone_numbers"));
            qparams.add(new BasicNameValuePair("limit", "1000"));
            URI uri = URIUtils.createURI("https", "mpdx.org", -1, "/api/v1/contacts",
                                         URLEncodedUtils.format(qparams, "UTF-8"), null);
            final URL location = uri.toURL();
            InputStream stream = null;

            try {
                Log.i(TAG, "Streaming data from network: " + location);
                stream = downloadUrl(location);
                final List<ContactsJSONParser.ContactEntry> entries = parseJSON(stream);
                updateLocalContactData(entries, syncResult, account);
                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Feed URL is malformed", e);
            syncResult.stats.numParseExceptions++;
            return;
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
        } catch (JSONException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            Log.e(TAG, e.getStackTrace()[0].toString());
            Log.e(TAG, e.getStackTrace()[1].toString());
            Log.e(TAG, e.getStackTrace()[2].toString());
            Log.e(TAG, e.getStackTrace()[3].toString());
            syncResult.databaseError = true;
            return;
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.databaseError = true;
            return;
        } catch (RemoteException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.databaseError = true;
            return;
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.databaseError = true;
            return;
        }
        Log.i(TAG, "Network synchronization complete");
    }

    private List<ContactsJSONParser.ContactEntry> parseJSON(InputStream stream) throws UnsupportedEncodingException, IOException, JSONException {
        final ContactsJSONParser contactsParser = new ContactsJSONParser();

        Log.i(TAG, "Parsing stream as JSON feed");
        return contactsParser.parse(stream);
    }

    protected void updateLocalContactData(final List<ContactsJSONParser.ContactEntry> entries,
                                          final SyncResult syncResult, final Account account)
            throws JSONException, IOException, OperationApplicationException, RemoteException {
        HashMap<Integer, ContactsJSONParser.ContactEntry> entryMap = new HashMap<Integer, ContactsJSONParser.ContactEntry>();
//      SparseArray<ContactsJSONParser.ContactEntry> entryMap = new SparseArray<ContactsJSONParser.ContactEntry>();
        for (ContactsJSONParser.ContactEntry e : entries) {
            entryMap.put(e.id, e);
        }

        // get existing entries
        Log.i(TAG, "Fetching local contacts for merge");
        Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
                .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
                .build();
        Cursor c = mContentResolver.query(rawContactUri, PROJECTION, null, null, null);
        assert c != null;
        Log.i(TAG, "Found " + c.getCount() + " local contacts. Computing merge solution...");

        // remove existing entries
        int id;
//      String firstName;
//      String lastName;
        while (c.moveToNext()) {
            syncResult.stats.numEntries++;
            id = c.getInt(0);
            ContactsJSONParser.ContactEntry match = entryMap.remove(id);
            if(match != null) {
                Log.d(TAG, match.firstName+" "+match.lastName+" exists.");
                // update old ones
            } else
                Log.d(TAG, "contact with id doesn't exist: "+id+" "+c.getColumnCount()+"columns");
        }

        // add new entries
//      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (ContactsJSONParser.ContactEntry e : entryMap.values()) {
            if(e.getPhoneNumbers().length != 0) {
                Log.d(TAG, "save: "+e.firstName+" "+e.lastName+" id:"+e.id);
                createContactEntry(e, account);
//              ops.addAll(generateCreateContactOperations(e, account));
//              mContentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
            }
        }
    }

    protected void createContactEntry(ContactsJSONParser.ContactEntry contact, Account account) {
        ArrayList<ContentProviderOperation> ops = generateCreateContactOperations(contact, account);

        // Ask the Contact provider to create a new contact
        Log.i(TAG,"Creating contact: " + contact.getFullName());
        try {
            getContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Ouch!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exceptoin encoutered while inserting contact: " + e);
        }
    }

    protected ArrayList<ContentProviderOperation> generateCreateContactOperations(
            ContactsJSONParser.ContactEntry contact, Account account) {
        String contact_id = Integer.toString(contact.id);
        String name = contact.getFullName();
        String[] phoneNumbers = contact.getPhoneNumbers();
        int phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;

        // Note: We use RawContacts because this data must be associated with a particular account.
        //       The system will aggregate this with any other data for this contact and create a
        //       coresponding entry in the ContactsContract.Contacts provider for us.
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
                .withValue(ContactsContract.RawContacts.SOURCE_ID, contact_id)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());
        for(int i = 0; i < phoneNumbers.length; i++) {
            String phone = phoneNumbers[i];
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                            .build());
        }
        return ops;
    }

    /**
     * Given a string representation of a URL, sets up a connection and gets an input stream.
     */
    private InputStream downloadUrl(final URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS);
        conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }
}
