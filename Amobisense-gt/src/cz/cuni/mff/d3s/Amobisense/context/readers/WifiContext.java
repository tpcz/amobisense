package cz.cuni.mff.d3s.Amobisense.context.readers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import cz.cuni.mff.d3s.Amobisense.context.ContextData;
import cz.cuni.mff.d3s.Amobisense.context.HistoryHolder;
import cz.cuni.mff.d3s.Amobisense.ui.BatteryLevelDetailInfoActivityMP;
import cz.cuni.mff.d3s.Amobisense.ui.MiscView;
import edu.umich.PowerTutor.phone.PhoneConstants;

/**
 * This is an example showing more complex scan. For simple examples refer to
 * {@link AccelerometerActivityContextReader} and {@link BatteryLevel}.
 * 
 * 
 * In order to create your own context watch, do the following:
 * =============================================================== * Make a copy
 * of some of example template * Change as needed * Register it in
 * PhoneSelector.generateContextReaders(); {@link BatteryLevel} and
 * {@link AccelerometerActivityContextReader}.
 * ================================================================ If store
 * data in {@link HistoryHolder} Object for context watch, data will be written
 * into log. But if you want to do it a little interesting for users, add a
 * GUI.. To do it, have a look in {@link MiscView} and e.g.
 * {@link BatteryLevelDetailInfoActivityMP}
 * ===============================================================
 * 
 * This file, as an example, returns battery level.
 * 
 * @author pop
 * 
 */
public class WifiContext extends AbstractEventReader {
	@SuppressWarnings("unused")
	private final String TAG = "WifiContext";
	private StringBuilder signalNameConstructor = new StringBuilder();
	private StringBuilder visibilityNameConstructor = new StringBuilder();
	public  List<ScanResult> results;

	private HashSet<String> allSeen = new HashSet<String>();

	Context c = null;

	public static WifiContext instance = null;

	public static WifiContext getInstance() {
		return instance;
	}

	public WifiContext(Context c, PhoneConstants phoneValues) {
		super(c, phoneValues,"NR-SEEN-WIFI");

		instance = this;
		this.c = c;
		
		// register receivers
		registerReceiver(new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		registerReceiver(new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		
		// perform initial scan perform scan..
		performScan(c);
		
		// start history
		rememberHistory();
	}

	public void clearResources() {
		unregisterReceiver();
		super.clearResources();
	}

	private Set<String> justSeen = new HashSet<String>();
	private static final long VISIBLE = 1L;
	private static final long NOT_VISIBLE = 0L;

	/** stores history for individual APs */
	private void storeSignalAndConnectionHistory(List<ScanResult> results) {

		justSeen.clear();
		visibilityNameConstructor.setLength(0);
		signalNameConstructor.setLength(0);

		synchronized (currdata) {
			for (ScanResult sr : results) {
				String vnc = "WIFI-" + sr.BSSID + "-" + sr.SSID + "-VISIBILITY";
				String snc = "WIFI-" + sr.BSSID + "-" + sr.SSID + "-SIGNAL";

				if (!currdata.containsKey(vnc)) {
					addResultDataItem(vnc);
					addResultDataItem(snc);
					allSeen.add(sr.BSSID + "-" + sr.SSID);
				}

				currdata.get(snc).setValue(sr.level);
				currdata.get(vnc).setValue(VISIBLE);

				justSeen.add(sr.BSSID + "-" + sr.SSID);
			}

			for (String key : allSeen) {
				if (!justSeen.contains(key)) {
					String vnc = "WIFI-" + key + "-VISIBILITY";
					String snc = "WIFI-" + key + "-SIGNAL";
					currdata.get(vnc).setValue(NOT_VISIBLE);
					currdata.get(snc).setValue(0L);
				}
			}
		}
	}

	private void performScan(Context c) {
		synchronized (this) {
			WifiManager manager;
			manager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
			
			if (manager != null) {
				results = manager.getScanResults();
			}else {
				results = null;
			}
		}
		
		if (results == null) {
			currdata.get(readerID).setValue(0);

		} else {
			currdata.get(readerID).setValue(results.size());
			storeSignalAndConnectionHistory(results);
		}
	}
	
	public List<ScanResult> getScanResults() {
		synchronized (this) {
			return results;
		}
	}

	@Override
	public void onReceive(Context c, Intent intent) {
		performScan(c);
	}


	@Override
	public String getReaderType() {
		return AbstractEventReader.TYPE_BROADCAST;
	}

	@Override
	public HashMap<String, HistoryHolder> getHistory() {
		return this.history;
	}

	@Override
	public boolean isSupported() {
		return this.results != null;
	}
}