package org.mpdx.nativecontacts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactsJSONParser {

	private JSONArray personArray;
	private JSONArray phoneNumberArray;

	public List<ContactEntry> parse(InputStream in)
			throws UnsupportedEncodingException, IOException, JSONException {
        try {
        	BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            JSONObject repsonseJSON = new JSONObject(responseStrBuilder.toString());

            final List<ContactEntry> contacts = new ArrayList<ContactsJSONParser.ContactEntry>();
            personArray = repsonseJSON.getJSONArray("people");
            phoneNumberArray = repsonseJSON.getJSONArray("phone_numbers");

            for (int i = 0; i < personArray.length(); i++) {
				JSONObject personJSON = personArray.getJSONObject(i);
				contacts.add(readPersonEntry(personJSON));
			}
            return contacts;
        } finally {
            in.close();
        }
    }

	private ContactEntry readPersonEntry(JSONObject entryJSON) throws JSONException {
		int id = entryJSON.getInt("id");
		String firstName = entryJSON.getString("first_name");
		String lastName = entryJSON.getString("last_name");
		String[] phoneNumbers = readPhoneNumbers(entryJSON.getJSONArray("phone_number_ids"));
		ContactEntry c = new ContactEntry(id, firstName, lastName);
		c.setPhoneNumbers(phoneNumbers);
		return c;
	}

	private String[] readPhoneNumbers(JSONArray phoneNumberIds) throws JSONException {
		String[] resp = new String[phoneNumberIds.length()];
		for (int i = 0; i < phoneNumberIds.length(); i++) {
			int phoneNumberId = phoneNumberIds.getInt(i);
			resp[i] = getPhoneNumberObjectFromId(phoneNumberId).getString("number");
		}
		return resp;
	}

	private JSONObject getPhoneNumberObjectFromId(int id) {
		for (int i = 0; i < phoneNumberArray.length(); i++) {
			try {
				JSONObject entry = phoneNumberArray.getJSONObject(i);
				if(entry.getInt("id") == id)
					return entry;
			} catch (JSONException e) {
				break;
			}
		}
		return null;
	}

	public static class ContactEntry {
		public int id;
		public String firstName;
		public String lastName;
		private String[] phoneNumbers;

		public ContactEntry(int id, String firstName, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String[] getPhoneNumbers() {
			return phoneNumbers;
		}
		public void setPhoneNumbers(String[] phoneNumbers) {
			this.phoneNumbers = phoneNumbers;
		}

		public JSONObject toContentJSON() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getFullName() {
			return firstName + " " + lastName;
		}
	}
}
