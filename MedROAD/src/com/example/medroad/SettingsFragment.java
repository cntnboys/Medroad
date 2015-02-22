// SettingsFragment.java
// Subclass of PreferenceFragment for managing app settings
package com.example.medroad;

import java.util.Set;



import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener  {   
	private static final String TAG = "SettingsFragment";
	
	private String [] btPairedNames = null;
	
	// creates preferences GUI from preferences.xml file in res/xml
   @Override
   public void onCreate(Bundle savedInstanceState) 
   {
	   
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences); // load from XML
      
      //set up choice array for device preferences
      btPaired();
      ListPreference ecgpref = (ListPreference) findPreference(MainActivity.PREF_ECG_NAME);
      if (ecgpref != null) {
    	  ecgpref.setEntries(btPairedNames);
    	  ecgpref.setEntryValues(btPairedNames);
      }
     
   } 
   
   @Override
   public void onResume() {
     super.onResume();
     getPreferenceScreen().getSharedPreferences()
     .registerOnSharedPreferenceChangeListener(this);
     for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
       Preference preference = getPreferenceScreen().getPreference(i);
       if (preference instanceof PreferenceGroup) {
         PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
         for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
           updatePreference(preferenceGroup.getPreference(j));
         }
       } else {
         updatePreference(preference);
       }
     }
   }

   @Override
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	   Log.i(TAG,"inside onsharedpref changed");
     updatePreference(findPreference(key));
   }

   public void updatePreference(Preference preference) {
     if (preference instanceof ListPreference) {
         ListPreference listPreference = (ListPreference) preference;
         listPreference.setSummary(listPreference.getEntry());
     } else if (preference instanceof EditTextPreference) {
    	 EditTextPreference editPreference = (EditTextPreference) preference;
    	 editPreference.setSummary(editPreference.getText());
     }
   }
   
   @Override
   public void onPause() {
       super.onPause();
       // Unregister the listener whenever a key changes
       getPreferenceScreen().getSharedPreferences()
           .unregisterOnSharedPreferenceChangeListener(this);
   }
   
   private void btPaired() {
		Set<BluetoothDevice> pairedDevices = MainActivity.mBluetoothAdapter.getBondedDevices();
	    // If there are paired devices
		int pindex = 0;
		int psize = pairedDevices.size();
		btPairedNames = new String[psize+1];
	    if (psize > 0) {
	        // Loop through paired devices
	        for (BluetoothDevice device : pairedDevices) {
	        	String name = device.getName();
	        	btPairedNames[pindex] = name;
	        	pindex += 1;
	        	Log.i(TAG, "found paired device " + name);
	        }
	    }
	    btPairedNames[pindex] = getString(R.string.none);
	}
} // end class SettingsFragment
