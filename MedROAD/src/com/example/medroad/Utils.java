package com.example.medroad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

public class Utils {
	
	public static StringBuilder hexString(byte[] buffer, int len) {
		StringBuilder hexstring = new StringBuilder(""); 
		
		for (int i = 0; i < len; i++) {
			hexstring.append(String.format("%02X ", buffer[i]));
		}	
		return hexstring;
	}
	
	public static String bitString(byte part) {
		String s1 = String.format("%8s", Integer.toBinaryString(part & 0xFF)).replace(' ', '0');
		return s1;
	}
		
	public static void waitABit(int howlong, String tag) {
		Log.i(tag,"waiting " + howlong);
		if (howlong > 0) {
			try {
				Thread.sleep(howlong);  
			} catch (Exception ex) {
				Log.e(tag," problem with sleep " + ex);
			}
		}
	}
	
	public static void waitABit(int howlong) {
		waitABit(howlong, "ECG Tester Utils");
	}
	
	// converts a byte array to an integer. If signed, it checks to see
		// if the number would be negative and converts accordingly
		// the byte array is ordered msb to lsb
		// Note if the reulsting number is too big for an int it will not work
		// in that case we need a new routine with a long
		public static int bytesToInteger (byte[] pnums, boolean signed) {
			int result = 0;
			if (signed && (pnums[0] < 0)) { //negative number use two's complement
				for (int i=0; i< pnums.length; i++) {
					result = (result << 8) + (~pnums[i] & 0xFF); //n;
				}
				result = (result + 1) * -1;
			} else { // positive number
				for (int i=0; i< pnums.length; i++) {
					result = (result << 8) + (pnums[i] & 0xFF); //n;
				}
			}
	    	return result;
		}
	
	// reads resources regardless of their size
	public static byte[] getFileContents(int id, Context context) throws IOException {
	    Resources resources = context.getResources();
	    InputStream is = resources.openRawResource(id);

	    ByteArrayOutputStream bout = new ByteArrayOutputStream();

	    byte[] readBuffer = new byte[4 * 1024];

	    try {
	        int readh;
	        do {
	            readh = is.read(readBuffer, 0, readBuffer.length);
	            if(readh == -1) {
	                break;
	            }
	            bout.write(readBuffer, 0, readh);
	        } while(true);

	        return bout.toByteArray();
	    } finally {
	        is.close();
	    }
	}
	
}