package com.example.medroad;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;



public class GlucoseActivity extends ActionBarActivity {

	private static final int REQUEST_ENABLE_BT = 0;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_glucose);
		
		
		// Initializes Bluetooth adapter.
		BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
		
		checkBluetooth();
		
		setupViewStuff();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.glucose, menu);
		return true;
	}
	
	
	
	private void checkBluetooth() {
		BluetoothAdapter mBluetoothAdapter = null;
		// Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
		// fire an intent to display a dialog asking the user to grant permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		    }
		}      
	}

	private void setupViewStuff() {
		/*
		Log.i(TAG,"setup view stuff " + connected + " " + reading);
		ecgNameTextView = (TextView) findViewById(R.id.ecgNameTextView);
		ecgNameTextView.setText("");
		pulseDataTextView = (TextView) findViewById(R.id.pulseDataTextView);
		pulseDataTextView.setText("");
			
		// get buttons so we can fiddle with them
		connectButton = (Button) findViewById(R.id.connectButton);
							
		// disable buttons until they are ready to use
		if (connected) {
			connectButton.setText(getString(R.string.stop_idle));
			connectButton.setEnabled(true);
			connectButton.setBackgroundColor(Color.RED);
		} else {
			if (connecting) { // in process of connecting
				connectButton.setText(getString(R.string.start));
				connectButton.setEnabled(false);
				connectButton.setBackgroundColor(Color.GRAY);
			} else {
				connectButton.setText(getString(R.string.start));
				connectButton.setBackgroundColor(Color.GREEN);
				connectButton.setEnabled(true);
			}
		}
		*/
		
	}

}
