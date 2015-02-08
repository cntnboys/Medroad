package com.example.medroad;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Set;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import ca.ammi.medlib.EmotionEcg;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

public class MainActivity extends ActionBarActivity {
	private static final String TAG = "EcgTester";
	
	private SharedPreferences sharedPreferences;
	public static final String PREF_ECG_NAME = "PREF_ECG_NAME";
	public static final String PREF_SIGNAL_RESOLUTION = "PREF_SIGNAL_RESOLUTION";
	public static final String PREF_HIGH_PASS_FILTER = "PREF_HIGH_PASS_FILTER";
	public static final String PREF_SAMPLING_FREQUENCY = "PREF_SAMPLING_FREQUENCY";
	public static final String PREF_Y_RANGE = "PREF_Y_RANGE";
	public static final String PREF_FILE_NAME = "PREF_FILE_NAME";
	public static final String PREF_SAVE_FILE = "PREF_SAVE_FILE";
	
	private Button connectButton;
	private TextView ecgNameTextView, pulseDataTextView;
	private View mainView = null;
	
	public String [] btPairedNames = null;
	public BluetoothDevice [] btPairedDevices = null;
	
	
	
	public static BluetoothManager bluetoothManager;
	public static BluetoothAdapter mBluetoothAdapter;
	private static final int REQUEST_ENABLE_BT = 1;
	private BluetoothDevice ecgDevice = null;
	private EmotionEcg emotionEcg = null;
	public String ECG_NAME = ""; // name of ecg device
	public int SIGNAL_RESOLUTION = 0; // note that signal resolution is actually /1000
	public int HIGH_PASS_FILTER = 0;
	public int SAMPLING_FREQUENCY = 1;
	public int Y_RANGE = 7000;
	public int useYrange = 7000;
	public boolean SAVE_FILE = false;
	public String FILE_NAME = "";
	public int graphMax = 1000; //1000 for portrait 2000 for landscape
	public int GRAPH_MAX_PORTRAIT = 1000; //1000 for portrait 
	public int GRAPH_MAX_LANDSCAPE = 2000; //2000 for landscape
	
	public final String DEFAULT_SIGNAL_RESOLUTION = "100";
	public final String DEFAULT_HIGH_PASS_FILTER = "1";
	public final String DEFAULT_SAMPLING_FREQUENCY = "100";
	public final String DEFAULT_Y_RANGE = "7000";
	public final String DEFAULT_FILE_NAME = "ecg-data";

	private int maxBeforeAdd = 75;
	private int sampleCount = 0;
	private int howOftenRedraw = 1;
	
	private int[] zeroEcgSample = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	
	private int lastPacketNum = -1;
	
	private boolean connected = false;
	private boolean connecting = false;
	private boolean reading = false;
	
	private Method handleEcgDataMethod = null;
	
	public boolean okToWrite = false;
	
	private String filename = "";
	private boolean writeFile = false;
	private FileOutputStream dataOut = null; 
	
	private Configuration currentConfig = null;
    
    private XYPlot ecgPlot;
    private SimpleXYSeries ecgSeries = null;
    LineAndPointFormatter ecgFormat;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG,"inside create");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
/*
		if (savedInstanceState == null) {
			PlaceholderFragment newf = new PlaceholderFragment();
			Log.i(TAG,"setting main view");
			mainView = newf.getView();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, newf).commit();
			//getSupportFragmentManager().beginTransaction()
			//.add(R.id.container, new PlaceholderFragment()).commit();
		} else {
			Log.i(TAG,"saved instance state - how to set mainView");
		}
		*/
		
		
		 // Initializes Bluetooth adapter.
		bluetoothManager =
		        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		checkBluetooth();
		
		if (isExternalStorageWritable()) {
			okToWrite = true;
		} else {
			okToWrite = false;
			Toast.makeText(this, "external storage not writable ", Toast.LENGTH_LONG).show();
			Log.e( TAG, "Could not write external storage");
		}
		
		currentConfig = getResources().getConfiguration();
		if (currentConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			graphMax = GRAPH_MAX_PORTRAIT;
		} else if (currentConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			graphMax = GRAPH_MAX_LANDSCAPE;
		}
		
		setupViewStuff();
		ecgPlotInit();
		
		 try { // get method for ecg to pass to ecg class
	            Class[] parameterTypes = new Class[1];
	            parameterTypes[0] = EmotionEcg.EcgData.class;
	            handleEcgDataMethod = MainActivity.class.getDeclaredMethod("handleEcgSampleData", parameterTypes);
	        	Class[] ptypes = handleEcgDataMethod.getParameterTypes();
	    		String sp = " ";
	    		for (int j=0; j<ptypes.length; j++) {
	    			sp = sp + ptypes[j] + " ";
	    		}
	    		Log.i(TAG,"setting ecg method " + handleEcgDataMethod.getName() + sp);
	        } catch (NoSuchMethodException e) {
	        	Log.i(TAG,"no ecg method " + e);
	        	handleEcgDataMethod = null;
	        }
		
	}

	private void setupViewStuff() {
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
	}
	
	private void ecgPlotInit() {
		Log.i(TAG,"ecgPlotInit");
		// this stuff doesn't change
		
		 // Create a formatter to use for drawing a series using LineAndPointRenderer
       // and configure it from xml:
       //LineAndPointFormatter 
       ecgFormat = new LineAndPointFormatter();
       ecgFormat.setPointLabelFormatter(new PointLabelFormatter());
       ecgFormat.configure(getApplicationContext(),
               R.xml.line_point_formatter_with_plf1);
       ecgFormat.setPointLabelFormatter(null);
       
       ecgSeries = new SimpleXYSeries("");
       ecgSeries.useImplicitXVals();
       
       ecgPlotSetup();
	}
	
	private void ecgPlotSetup() {
       
       ecgPlot = (XYPlot) findViewById(R.id.ecgPlot);
       // add a new series' to the xyplot:
       ecgPlot.addSeries(ecgSeries, ecgFormat); 
    // ecgPlot.addSeries(ecgSeries, new LineAndPointFormatter(Color.rgb(100, 100, 200), Color.BLACK, null));
       
       //ecgPlot.setTicksPerRangeLabel(10);
       ecgPlot.setRangeValueFormat(new DecimalFormat("#"));
       
       ecgPlot.getGraphWidget().setDomainLabelOrientation(-45);
       
       //ecgPlot.setTicksPerDomainLabel(10);
       //ecgPlot.setDomainValueFormat(new DecimalFormat("#"));
       // make domain legend blank (better than squished or meaningless numbers)
       ecgPlot.setDomainValueFormat(new Format() {
    	   
           @Override
           public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
               return new StringBuffer("");
           }

           @Override
           public Object parseObject(String source, ParsePosition pos) {
               return null;

           }
       });
       
      // ecgPlot.getLayoutManager().remove(ecgPlot.getDomainLabelWidget());
      // ecgPlot.getLayoutManager().remove(ecgPlot.getRangeLabelWidget());
       //ecgPlot.disableAllMarkup();
      
       //ecgPlot.removeMarkers();
       
       //make the background lines disappear
       ecgPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
       ecgPlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.WHITE);
       ecgPlot.getGraphWidget().getRangeGridLinePaint().setColor(Color.WHITE);
       
       // this stuff might
       ecgPlotReinit();
	}
		
	
	private void ecgPlotReinit() {
        Log.i(TAG,"ecgPlotReinit " + graphMax);
        howOftenRedraw = SAMPLING_FREQUENCY / 100; //keep redraw rate same even with different sampling rate
		maxBeforeAdd = (graphMax / 1000) * SAMPLING_FREQUENCY - 25;
     // Domain
		// ensure that data still displays the same time wise even with higher sampling
        ecgPlot.setDomainStep(XYStepMode.SUBDIVIDE, graphMax/SAMPLING_FREQUENCY);
        //ecgPlot.setDomainStep(XYStepMode.SUBDIVIDE, 1000/SAMPLING_FREQUENCY);
        
      //Range
        //ecgPlot.setRangeBoundaries(-7000, 7000, BoundaryMode.FIXED);
        ecgPlot.setRangeBoundaries(- useYrange, useYrange, BoundaryMode.FIXED);
        //ecgPlot.setRangeBoundaries(- Y_RANGE, Y_RANGE, BoundaryMode.FIXED);
        ecgPlot.setRangeStepValue(11);
    
	}
	
	
	@Override
    protected void onResume() {
        super.onResume();
        //recheck bluetooth status in case turned off ? is this really needed?
        checkBluetooth();
        
        Log.i(TAG,"resuming " + connected + " " + reading);
        
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ECG_NAME = sharedPreferences.getString(PREF_ECG_NAME,"");
    	SIGNAL_RESOLUTION = Integer.parseInt(sharedPreferences.getString(PREF_SIGNAL_RESOLUTION,DEFAULT_SIGNAL_RESOLUTION));
    	HIGH_PASS_FILTER = Integer.parseInt(sharedPreferences.getString(PREF_HIGH_PASS_FILTER,DEFAULT_HIGH_PASS_FILTER));
    	SAMPLING_FREQUENCY = Integer.parseInt(sharedPreferences.getString(PREF_SAMPLING_FREQUENCY,DEFAULT_SAMPLING_FREQUENCY));
    	Y_RANGE = Integer.parseInt(sharedPreferences.getString(PREF_Y_RANGE,DEFAULT_Y_RANGE));
    	FILE_NAME = sharedPreferences.getString(PREF_FILE_NAME, DEFAULT_FILE_NAME);
    	SAVE_FILE = sharedPreferences.getBoolean(PREF_SAVE_FILE,false);
        useYrange = Y_RANGE;
        if (SIGNAL_RESOLUTION == 20) { //(SIGNAL_RESOLUTION == 100) {
        	useYrange = (int) (useYrange * 6.0f); //(useYrange * .2f);
        }
        if (HIGH_PASS_FILTER == 10) {
        	useYrange = (int) (useYrange * .6f);
        }
    	Log.i(TAG,"ecg " + ECG_NAME + "\n"
        		+ "  signal resolution " + SIGNAL_RESOLUTION + "\n"
        		+ "  high pass filter " + HIGH_PASS_FILTER + "\n"
        		+ "  sampling frequency " + SAMPLING_FREQUENCY  + "\n"
        		+ "  yrange " + Y_RANGE + " " + useYrange + "\n"
        		+ "  " + SAVE_FILE + " " + FILE_NAME);
        
        
        if (!connected) {
        	setPaired(); 
        }
        
        if (!reading) {
        	setEcgInfo();
        	// ecgNameTextView.setText(ECG_NAME + "   " + (SIGNAL_RESOLUTION/100f) + " muV/count   "
            //		+ HIGH_PASS_FILTER + " Hz   " + SAMPLING_FREQUENCY + " Hz" );
   
        }
    }
	
	private void setEcgInfo() {
		/*
		if (currentConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			ecgNameTextView.setText(ECG_NAME + "   SR: " + (SIGNAL_RESOLUTION/100f) + " muV/count   HPF: "
					 + HIGH_PASS_FILTER + " Hz   SF: " + SAMPLING_FREQUENCY + " Hz" );
		} else if (currentConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			ecgNameTextView.setText(ECG_NAME + "   SR: " + (SIGNAL_RESOLUTION/100f) + " muV/count   "
					+ "\n" + "HPF: " + HIGH_PASS_FILTER + " Hz   SF: " + SAMPLING_FREQUENCY + " Hz"  );
		}*/
		ecgNameTextView.setText(ECG_NAME + "   SR: " + (SIGNAL_RESOLUTION/100f) + " muV/count   "
				+ "\n" + "HPF: " + HIGH_PASS_FILTER + " Hz   SF: " + SAMPLING_FREQUENCY + " Hz"  );
		//ecgNameTextView.setText(ECG_NAME + "   " + (SIGNAL_RESOLUTION/100f) + " muV/count   "
			//	+ "\n" + HIGH_PASS_FILTER + " Hz   " + SAMPLING_FREQUENCY + " Hz" );
	}
	
	// Bluetooth stuff
	private void checkBluetooth() {
		// Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
		// fire an intent to display a dialog asking the user to grant permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		    }
		}      
	}
		
	private void setPaired() {
		// reset devices in case something changed 
		ecgDevice = null;
		emotionEcg = null;
			
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		int psize = pairedDevices.size();
			
		if (psize > 0) {
			// Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // check name Polar = heart monitor, Nonin = oximeter
		        String name = device.getName();
		        if (name.equals(ECG_NAME)) {
			        ecgDevice = device;
			        emotionEcg = new EmotionEcg(ecgDevice, ecgHandler, handleEcgDataMethod, this);
			        //emotionEcg.setSeries(ecgSeries);
			        //emotionEcg.setPlot(ecgPlot);
			    }
		    }
		}    
	}
		
	
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent preferencesIntent = new Intent(this, SensorReadPreferenceActivity.class);
            startActivity(preferencesIntent); 
			return true;
		} else if (id == R.id.action_help) {
			displayHelpFile(); 
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			Log.i(TAG,"onCreateView");
			return rootView;
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentConfig = newConfig;

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i(TAG,"changed to portrait");
            graphMax = GRAPH_MAX_PORTRAIT;
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        	Log.i(TAG,"changed to landscape");
        	graphMax = GRAPH_MAX_LANDSCAPE;
        }
        setContentView(R.layout.activity_main);
        setupViewStuff();
		ecgPlotSetup();
		setEcgInfo();
		
		
    }
	
	/** Called when the user clicks the Connect button */
	public void ecgConnect(View view) {
		Log.i(TAG,"in connect");
		if (connectButton.getText().equals(getString(R.string.stop_idle))) {
			ecgDisconnect(view);
		} else {
			doEcgConnect(view);
		}
	}
	
	public void doEcgConnect(View view) {
		if (emotionEcg != null) {
			lastPacketNum = -1;
			connecting = true;
			connectButton.setEnabled(false);
			connectButton.setBackgroundColor(Color.GRAY);
			//connected = true;
			// set up output file for writing
        	if (SAVE_FILE && okToWrite) {
        		//if (emotionEcg != null) {
        			//emotionEcg.setWriteFile(FILE_NAME);
        			setWriteFile(FILE_NAME);
        		//}
        	}
			emotionEcg.connect();
		} else {
			Toast.makeText(MainActivity.this, "Select an ECG device in Settings", Toast.LENGTH_SHORT).show();
		}
	}
	
	/** Called when the user clicks the Disconnect button */
	public void ecgDisconnect(View view) {
		Log.i(TAG,"in disconnect");
		connected = false;
		stopIdle();
		if (emotionEcg != null) {
			emotionEcg.disconnect();
		}
		connectButton.setText(getString(R.string.start));
		connectButton.setBackgroundColor(Color.GREEN);
		connectButton.setEnabled(true);
	}

	/** Called when the user clicks the start button */

	
	public void doStartRead() { //View view) {
		if (emotionEcg != null) {
			setupRead();
			reading = true;
			//setEcgParameters();
			//emotionEcg.startReadFormat07();
			Log.i(TAG,"before setupand startread");
			emotionEcg.setupAndStartRead();
			Log.i(TAG,"after setupand startread");
			//Utils.waitABit(500, "ECG");
			//emotionEcg.getData();
		}
	}
	
	public void setupRead() {
		setEcgInfo();
		//ecgNameTextView.setText(ECG_NAME + "   " + (SIGNAL_RESOLUTION/100f) + " muV/count   "
        //		+ HIGH_PASS_FILTER + " Hz   " + SAMPLING_FREQUENCY + " Hz" );
		if (emotionEcg != null) {
    		emotionEcg.setSamplingFrequency(SAMPLING_FREQUENCY);
    		emotionEcg.setHighPassFilter(HIGH_PASS_FILTER);
    		emotionEcg.setSignalResolution(SIGNAL_RESOLUTION);
    	}
		 //initialize graph stuff
        ecgPlotReinit();
        // set up output file for writing
    	if (SAVE_FILE && okToWrite) {
    		setWriteFile(FILE_NAME);
    		//if (emotionEcg != null) {
    		//	emotionEcg.setWriteFile(FILE_NAME);
    		//}
    	}
	}

	/** Called when the user clicks the stopIdle button */
	public void stopIdle() { //View view) {
		if (emotionEcg != null) {
			reading = false;
			emotionEcg.stopData();
			emotionEcg.stopAndIdle();
		}
	}
	

	Handler ecgHandler = new Handler(new IncomingEmotionHandlerCallback());

	class IncomingEmotionHandlerCallback implements Handler.Callback{
		BluetoothDevice device;

	    @Override
	    public boolean handleMessage(Message msg) {
	    	switch (msg.what) { 
	    	case EmotionEcg.GET_DATA: 
	    		int pulse = msg.arg1;
	    		//Log.i(TAG,"got pulse " + pulse);
	    		pulseDataTextView.setText(" " + pulse);
	    		break;
	    	case EmotionEcg.ECG_SAMPLES: 
	    		// now handled by ecg thread
	    		break;
	    	case EmotionEcg.STOP_DATA:
	    		// have to stop reading from device and user didn't ask for it
	    		Log.i(TAG,"stop reading "  );
	    		reading = false; 
	    		break;
	    	case EmotionEcg.BATTERY_LOW:
	    		// battery low
	    		Log.i(TAG,"ecg battery low "  );
	    		Toast.makeText(MainActivity.this, "Battery low ", Toast.LENGTH_SHORT).show();
	    		break;
	    	case EmotionEcg.CONNECTED_BT: 
	    		// we connected to device
	    		device = (BluetoothDevice) msg.obj;
		    	Log.i(TAG,"connected to bluetooth device " + device.getName().toString());
		    	connectButton.setText(getString(R.string.stop_idle));
		    	connectButton.setBackgroundColor(Color.RED);
		    	connectButton.setEnabled(true);
	    		connected = true;
	    		connecting = false;
	    		doStartRead();
	    		break;
	    	case EmotionEcg.CANNOT_CONNECT_BT:
	    		// we couldn't connect to device
	    		boolean givemessage = true;
	    		if (givemessage) {
	    			//tryingAllUuids = false;
	    			device = (BluetoothDevice) msg.obj;
	    			Log.i(TAG,"cannot connect to bluetooth device " + device.getName().toString());
	    			Toast.makeText(MainActivity.this, "Cannot connect to bluetooth device " + device.getName().toString(), Toast.LENGTH_LONG).show();
	    			connectButton.setText(getString(R.string.start));
	    			connectButton.setBackgroundColor(Color.GREEN);
	    			connectButton.setEnabled(true);
	    			connecting = false;
	    		}
	    		break;
	    	default: 
	    		 Log.i(TAG,"message handler " + msg.toString() + msg.what);
	    	}
	    	 return true;
	    }
	}
	
	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	private void doHandle(int[] sampl, int pnew) {
		updateEcgSeries(sampl,pnew);
		writeFileData(sampl);
	}
	
	// invoked from edg thread
	public void handleEcgSampleData(EmotionEcg.EcgData newdata) {
		int[] sampl = null;
		int pnew = 0;
		if (newdata != null) {
			sampl = newdata.getSamples();
			pnew = newdata.getPacketNumber();
			//Log.i(TAG,"handling data packet " + pnew);
			if ((lastPacketNum > 0) && ((pnew - lastPacketNum) > 1)) {
    			Log.w(TAG, " Missing packet numbers " + (lastPacketNum + 1) + " to " + (pnew - 1));
    			//fill in with 0's
    			for (int i=lastPacketNum+1; i<=pnew-1; i++){
    				Log.w(TAG,"filling in packet " + i + "with zeros");
    				doHandle(zeroEcgSample, i);
    			}
    		} else if (pnew < lastPacketNum) {
    			Log.w(TAG," Packet out of order " + pnew );
    		}
    		if (pnew > lastPacketNum) {
    			lastPacketNum = pnew;
    		}
    		doHandle(sampl, pnew);
		}
	}
	
	

	// file stuff
		
	public void setWriteFile(String f) {
		Log.i(TAG,"set file " + f);
		if (dataOut != null) { // close last file if there is one
			closeDataFile(dataOut);
		}
		filename = f;
		writeFile = true;
		dataOut = getDataFile(f);
	}
		
	public void shrinkSeries() {
		// get rid the oldest samples in history:
	       while (ecgSeries.size() > maxBeforeAdd) { 
	           ecgSeries.removeFirst();
	       }
	}
		
	//update series used for graph, then redraw graph if necessary
	private void updateEcgSeries(int[] samples, int packetNum) {
		//Log.i(TAG,"update ecg series " + samples.toString());
		// get rid the oldest samples in history:
			
		//check packet num to see if missing any - if so, handle
	    while (ecgSeries.size() > maxBeforeAdd) { 
	       ecgSeries.removeFirst();
	    }
			
	    // add current data
		for (int index = 0; index < samples.length; index++) {
		    ecgSeries.addLast(null,  samples[index]);
		}
		    
		// Try to keep the rate of redrawing the same even with a higher sampling rate and more data
		sampleCount += 1;
		if (ecgPlot != null) {
			if ((sampleCount % howOftenRedraw) == 0) { // redraw about same rate
				//Log.i(TAG,"redraw " + sampleCount + " " + howOftenRedraw );
				ecgPlot.redraw();
				if (sampleCount > 30000) {  // keep the number from getting too big
					sampleCount = 1;
				}
			}
		}
	}
		
	private FileOutputStream getDataFile(String filename) {
		// Get the directory for documents. 
		//File file = new File(Environment.getExternalStoragePublicDirectory(
		//		Environment.DIRECTORY_DOCUMENTS), fileName);  // only in 19 can't use
		// external file
		File file = new File(Environment.getExternalStoragePublicDirectory(
		    	Environment.DIRECTORY_DOWNLOADS), filename);
		//File file = new File(Environment.getExternalStorageDirectory(), filename);
		//File file = new File(Environment.getDataDirectory(), "com.example.blueplay." + filename);
		//File file = new File(this.getFilesDir(), filename);
		//File file = new File(filename);
		Log.i(TAG, "Filename is " + file.toString());
		FileOutputStream ostream = null;
		//Toast.makeText(this, "file:" + file.toString(), Toast.LENGTH_LONG).show();
		if (!file.canWrite()) {
		    Log.e(TAG, "Could not write output file");
		} 
		/*
		if (!file.mkdirs()) {
		    Log.e(TAG, file.toString() + " Directory not created");
		    Toast.makeText(this, "Directory not created", Toast.LENGTH_SHORT).show();
		} */
		if (file != null) {
		    try {
		    	//ostream = openFileOutput(filename, Context.MODE_PRIVATE); // internal file
		    	ostream = new FileOutputStream(file);
		    }
		    catch (IOException ex){
				Log.e(TAG, "Could not get output file stream", ex );
			} 
		}
		return ostream;
	}
		
	private void writeFileData (int[] samples) {
		String s = "";
		for (int i=0; i<25; i++) {
			s = s + samples[i] + ", ";
		}
		byte[] bytes = s.getBytes();
		//Log.i(TAG,"samples s " + s);
		writeData(bytes, bytes.length);
	}
		
	// write data to file
		private void writeData(byte[] buffer, int bytes) {
			if (dataOut != null) {
				try {
					//append data
					dataOut.write(buffer, 0, bytes);
					//Log.i(TAG, "writing " + buffer.toString());
				} 
				catch (IOException ex){
					Log.e( TAG, "Could not write to file", ex );
				} 	
			}
		}
			
	private void closeDataFile(FileOutputStream fileOut) {
		try {
	    	Log.i(TAG,"closing output file");
	    	fileOut.close();
	    	}
	    catch(IOException ex){
	    	Log.e( TAG, "Could not close file", ex );
	    }
	}
		
	private void displayHelpFile() {
		try {
			byte [] gothelp = Utils.getFileContents(R.raw.help, this);
			String helpstring = new String(gothelp);
			infoDialog(getString(R.string.action_help), helpstring);
		} catch (Exception e) {
			Log.i(TAG,"could not get help file");
			infoDialog(getString(R.string.action_help), "Sorry, help is not available");
		}
	}
	
	private void infoDialog (String title, String message) {
		// create a new AlertDialog Builder
		AlertDialog.Builder builder = 
				new AlertDialog.Builder(this);
		builder.setTitle(title); 

		// set dialog's message to display
		builder.setMessage(Html.fromHtml(message));
		//builder.setMessage(message);
    
		// provide an OK button that simply dismisses the dialog
		builder.setPositiveButton(R.string.OK, null); 
    
		// create AlertDialog from the AlertDialog.Builder
		AlertDialog infoDialog = builder.create();
		infoDialog.show(); // display the modal dialog
	}
	
	
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();  // Always call the superclass method first
	    if (emotionEcg != null) {
	    	emotionEcg.cleanup();
	    }
	/*
	    if (btDevice != null) {
	    	btDevice.cleanup();
	    }
	    if (emotionEcg != null) {
	    	emotionEcg.cleanup();
	    }*/
	    if (dataOut != null) {
	    	// close it
	    	try {
	    		Log.i(TAG,"closing output file");
	    		dataOut.close();
	    	}
	    	catch(IOException ex){
	    		Log.e( TAG, "Could not close file", ex );
	    	}
	    }
	}
	
}
