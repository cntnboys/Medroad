package com.example.medroad;


import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;


public class SensorReadPreferenceActivity extends PreferenceActivity {

	 // use FragmentManager to display SettingsFragment
	 @Override
	 protected void onCreate(Bundle savedInstanceState) 
	 {
		 Log.i("SRPA","inside create sensorreadprefact");
	     super.onCreate(savedInstanceState);
	     Log.i("SRPA","afteroncreate");
	     setContentView(R.layout.activity_settings);
	     Log.i("SRPA","after setcontent");
	 }

}