package cz.cuni.mff.d3s.Amobisense.context.readers;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import cz.cuni.mff.d3s.Amobisense.context.HistoryHolder;
import cz.cuni.mff.d3s.Amobisense.ui.BatteryLevelDetailInfoActivityMP;
import cz.cuni.mff.d3s.Amobisense.ui.MiscView;
import edu.umich.PowerTutor.phone.PhoneConstants;


/**
 * This is an example showing how to create your own context watch
 * reading BROADCAST LISTENERS. If you want to periodically read
 * a particular sensor, have a look in {@link AccelerometerActivityContextReader}.
 * 
 * 	In order to create your own context watch, do the following:
 * ===============================================================
 * * Make a copy of some of example template
 * * Change as needed
 * * Register it in PhoneSelector.generateContextReaders();
 * ================================================================
 * If store data in {@link HistoryHolder} Object for context watch, data will be
 * written into log. But if you want to do it a little interesting for users,
 * add a GUI..	To do it, have a look in 
 * {@link MiscView} and e.g. 
 * {@link BatteryLevelDetailInfoActivityMP}  
 *  ===============================================================
 * 
 * 
 * This file, as an example, returns battery level.
 * 
 * @author pop
 *
 */
public class BatteryLevel extends AbstractEventReader {
	@SuppressWarnings("unused")
	private final String TAG = "BatteryContext";
	
	Intent receiver = null;
	Context c = null;

	public static BatteryLevel instance = null;

	public static BatteryLevel getInstance() {
		return instance;
	}

	public BatteryLevel(Context c, PhoneConstants phoneValues) {
		super(c, phoneValues, "BATTERY-LEVEL");
		instance = this;
		
		// register all your receivers here...
		registerReceiver(new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		performScan(c, c.registerReceiver(null, new IntentFilter( Intent.ACTION_BATTERY_CHANGED)));

		// start history holder
		rememberHistory();
	}

	public void clearResources() {
		super.clearResources();
		// unregister your receivers here ...
		unregisterReceiver();

	}

	private void performScan(Context c, Intent i) {
		currdata.get(readerID).setValue(i.getIntExtra("level", 0));
	}

	@Override
	public void onReceive(Context c, Intent i) {
		performScan(c, i);
	}

	@Override
	public String getReaderType() {
		return AbstractEventReader.TYPE_BROADCAST;
	}

	@Override
	public HashMap<String, HistoryHolder> getHistory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSupported() {
		return true;
	}

}