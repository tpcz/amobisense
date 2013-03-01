package cz.cuni.mff.d3s.Amobisense.context.readers;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import cz.cuni.mff.d3s.Amobisense.context.ContextData;
import cz.cuni.mff.d3s.Amobisense.context.HistoryHolder;
import cz.cuni.mff.d3s.Amobisense.ui.BatteryLevelDetailInfoActivityMP;
import cz.cuni.mff.d3s.Amobisense.ui.MiscView;
import edu.umich.PowerTutor.phone.PhoneConstants;

/**
 * In order to create your own context watch, do the following:
 * =============================================================== * Make a copy
 * of some of example template * Change as needed * Register it in
 * PhoneSelector.generateContextReaders();
 * 
 * Simple examples of context watches could be found in files
 * {@link BatteryLevel} and {@link AccelerometerActivityContextReader}.
 * ================================================================ If store
 * data in {@link HistoryHolder} Object for context watch, data will be written
 * into log. But if you want to do it a little interesting for users, add a
 * GUI.. To do it, have a look in {@link MiscView} and e.g.
 * {@link BatteryLevelDetailInfoActivityMP}
 * ===============================================================
 * 
 * 
 * This file, as an example, returns battery level.
 * 
 * @author pop
 * 
 */
public class InternetConnection extends AbstractEventReader {
	@SuppressWarnings("unused")
	private final String TAG = "Internet Connection";

	ConnectivityManager cm;
	WifiManager wm;

	public static final int CONNECTION_TYPE_NOT_CONNECTED = 0;
	public static final int CONNECTION_TYPE_MOBILE = 1;
	public static final int CONNECTION_TYPE_WIFI = 2;
	

	public static InternetConnection instance = null;

	public static InternetConnection getInstance() {
		return instance;
	}

	public InternetConnection(Context c, PhoneConstants phoneValues) {
		super(c, phoneValues, "NET-CONNECTION");
		intValuesMapping = new String[]{"NO", "MOBILE", "WIFI"};
		instance = this;

		cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
		wm = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);;

		// register all your receivers here...
		registerReceiver(new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

		// perform initial scan perform scan..
		// (register Receiver with null will return the last Intent for Sticky
		// Broadcasts.. )
		performScan(c, c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));

		// start history holder
		rememberHistory();
	}

	/**
	 * 
	 * @return 0 - not connected, 1 - Mobile net, 2 - wifi
	 */
	private int getNetState() {
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork != null) {
			boolean isConnected = activeNetwork.isConnectedOrConnecting();
			if (!isConnected) {
				return CONNECTION_TYPE_NOT_CONNECTED;
			} else {
				int ret;
				if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
					ret = CONNECTION_TYPE_WIFI;
				} else {
					ret = CONNECTION_TYPE_MOBILE;
				}
				// WIFI, MOBILE. Non zero
				return ret;

			}
		} else {
			return CONNECTION_TYPE_NOT_CONNECTED;
		}
	}

	public void clearResources() {
		super.clearResources();
		// unregister your receivers here ...
		unregisterReceiver();

	}

	/*
	 * private void performScan (Context c, Intent i) {
	 * 
	 * Bundle extras = i.getExtras(); if (extras != null) { Object netInfo =
	 * extras.get("networkInfo"); if (netInfo != null) {
	 * this.currdata.setValue(extras.get("networkInfo").toString(),
	 * getNetState()); }else { this.currdata.setValue("No info", getNetState());
	 * } } else { this.currdata.setValue("No Info", getNetState()); } }
	 */

	private void performScan(Context c, Intent i) {
		int netState = getNetState();
		
		if (netState == CONNECTION_TYPE_WIFI) {
			if (wm != null && wm.getConnectionInfo() != null) {
				WifiInfo wi = wm.getConnectionInfo();
				this.currdata.get(readerID).setValue(wi.getSSID() + " " + wi.getBSSID(), getNetState());
				return;
			}
		}
		this.currdata.get(readerID).setValue("?", getNetState());
	}

	@Override
	public void onReceive(Context c, Intent i) {
		performScan(c, i);
	}

	@Override
	public String getReaderType() {
		return AbstractEventReader.TYPE_PERIODIC;
	}

	@Override
	public HashMap<String, HistoryHolder> getHistory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSupported() {
		return wm != null && cm != null;
	}
	
	

}