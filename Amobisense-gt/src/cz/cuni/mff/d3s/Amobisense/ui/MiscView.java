/*
Copyright (C) 2011 The University of Michigan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please send inquiries to powertutor@umich.edu
 */

package cz.cuni.mff.d3s.Amobisense.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cz.cuni.mff.d3s.Amobisense.R;
import cz.cuni.mff.d3s.Amobisense.context.readers.AbstractReader;
import cz.cuni.mff.d3s.Amobisense.context.readers.Accelerometer;
import cz.cuni.mff.d3s.Amobisense.context.readers.BatteryLevel;
import cz.cuni.mff.d3s.Amobisense.context.readers.GSMCells;
import cz.cuni.mff.d3s.Amobisense.context.readers.InternetConnection;
import cz.cuni.mff.d3s.Amobisense.context.readers.Light;
import cz.cuni.mff.d3s.Amobisense.context.readers.Orientation;
import cz.cuni.mff.d3s.Amobisense.context.readers.Proximity;
import cz.cuni.mff.d3s.Amobisense.context.readers.ScreenOrintationAndBrightness;
import cz.cuni.mff.d3s.Amobisense.context.readers.Temperature;
import cz.cuni.mff.d3s.Amobisense.context.readers.WifiContext;
import cz.cuni.mff.d3s.Amobisense.service.MainBackgroundService;
import edu.umich.PowerTutor.dataReaders.CPU;
import edu.umich.PowerTutor.service.DataCollector;
import edu.umich.PowerTutor.service.ICounterService;
import edu.umich.PowerTutor.util.BatteryStats;
import edu.umich.PowerTutor.util.SystemInfo;

public class MiscView extends Activity {
	private static final String TAG = "MiscView";

	private int uid;
	private Runnable collector;
	SharedPreferences prefs;

	private Intent serviceIntent;
	private CounterServiceConnection conn;
	private ICounterService counterService;
	private Handler handler;

	private ArrayAdapter<InfoItem> adapter;

	private BatteryStats batteryStats;
	private String[] componentNames;
	private static MiscView instance = null;
	final ArrayList<InfoItem> allItems = new ArrayList<InfoItem>();

	public static MiscView getInstance() {
		return instance;
	}

	public void addComponents() {

		allItems.add(new GSMCellItem());
		allItems.add(new AccelerometerItem());
		allItems.add(new ProximityItem());
		allItems.add(new OrientationItem());
		allItems.add(new ScreenInfoItem());

		allItems.add(new LightItem());
		allItems.add(new GenericInfoItem("Temperature", "C", Temperature.getInstance(), "C"));
		allItems.add(new InternetConnectionItem());
		allItems.add(new WifiItem());
		allItems.add(new CPUUSageItem());
		allItems.add(new BatteryTempItem());
		allItems.add(new BatteryLevelItem());
	}

	public void refreshView() {
		final ListView listView = new ListView(this);

		adapter = new ArrayAdapter<InfoItem>(this, 0) {

			public View getView(int position, View convertView, ViewGroup parent) {
				View itemView = getLayoutInflater().inflate(R.layout.misc_item_layout, listView, false);
				TextView title = (TextView) itemView.findViewById(R.id.title);
				TextView summary = (TextView) itemView.findViewById(R.id.summary);
				LinearLayout widgetGroup = (LinearLayout) itemView.findViewById(R.id.widget_frame);
				InfoItem item = (InfoItem) getItem(position);

				item.initViews(title, summary, widgetGroup);

				item.setupView();

				return itemView;
			}
		};

		for (InfoItem inf : allItems) {
			if (inf.available()) {
				adapter.add(inf);
			}
		}

		listView.setAdapter(adapter);
		setContentView(listView);

		listView.setTextFilterEnabled(true);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

				InfoItem item = (InfoItem) adapter.getItem(position);
				
				if (item.onClickActivityClazz != null) {
					Intent i = new Intent(MiscView.this, item.onClickActivityClazz);
					if (item.onClickActivityIntentExtras != null) {
						i.putExtras(item.onClickActivityIntentExtras);
					}

					MiscView.this.startActivity(i);
				} else {
					Toast.makeText(getBaseContext(), "No details available on this item", Toast.LENGTH_SHORT).show();
				}
			}
		});

		collector = new Runnable() {
			public void run() {
				for (InfoItem inf : allItems) {
					if (inf.available()) {
						inf.setupView();
					}
				}
				if (handler != null) {
					handler.postDelayed(this, 2 * DataCollector.ITERATION_INTERVAL);
				}
			}

		};

		if (handler != null) {
			handler.post(collector);
		}

	}

	class CounterServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className, IBinder boundService) {
			counterService = ICounterService.Stub.asInterface((IBinder) boundService);
			try {
				componentNames = counterService.getComponents();
			} catch (RemoteException e) {
				componentNames = new String[0];
			}
			refreshView();
		}

		public void onServiceDisconnected(ComponentName className) {
			counterService = null;
			getApplicationContext().unbindService(conn);
			getApplicationContext().bindService(serviceIntent, conn, 0);
			Log.w(TAG, "Unexpectedly lost connection to service");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		uid = getIntent().getIntExtra("uid", SystemInfo.AID_ALL);
		if (savedInstanceState != null) {
			componentNames = savedInstanceState.getStringArray("componentNames");
		}
		batteryStats = BatteryStats.getInstance();
		serviceIntent = new Intent(this, MainBackgroundService.class);
		conn = new CounterServiceConnection();
		addComponents();
	}

	@Override
	protected void onResume() {
		super.onResume();
		handler = new Handler();
		getApplicationContext().bindService(serviceIntent, conn, 0);
		refreshView();
	}

	@Override
	protected void onPause() {
		super.onPause();
		getApplicationContext().unbindService(conn);
		if (collector != null) {
			handler.removeCallbacks(collector);
			collector = null;
			handler = null;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArray("componentNames", componentNames);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return null;
	}

	private abstract class InfoItem {
		protected TextView title;
		protected TextView summary;
		protected TextView textView;
		protected Boolean prefDisabled  = false;

		@SuppressWarnings("rawtypes")
		protected Class onClickActivityClazz = null;
		protected Bundle onClickActivityIntentExtras = null;

		public void initViews(TextView title, TextView summary, LinearLayout widgetGroup) {
			this.title = title;
			this.summary = summary;
			textView = new TextView(MiscView.this);
			widgetGroup.addView(textView);
		}

		public abstract boolean available();

		public abstract void setupView();
	}

	private class GenericInfoItem extends InfoItem {

		protected AbstractReader r = null;
		
		protected String units = null;
		protected String titleString = null;
		protected String summaryString = null;

		public void initViews(TextView title, TextView summary, LinearLayout widgetGroup) {
			this.title = title;
			this.summary = summary;
			textView = new TextView(MiscView.this);
			widgetGroup.addView(textView);
		}

		public GenericInfoItem(String title, String summary, AbstractReader r, String units, Class showActClazz) {
			this.r = r;
			this.onClickActivityClazz = showActClazz;
			
			this.units = units;
			this.titleString = title;
			this.summaryString = summary;
		}
		
		public GenericInfoItem(String title, String summary, AbstractReader r, String units)  {
			this(title, summary, r, units, null);
		}

		public boolean available() {
			return (null != r && r.isSupported());
		}

		public void setupView() {
			if (textView == null)
				return;
					String info;
			info = String.format("%.2f %s", r.getCurrentMainData().toDouble(), units);

			textView.setText(info);
			textView.setGravity(Gravity.CENTER);

			title.setText(titleString);
			summary.setText(summaryString);
		}
	}

	/**
	 * This a sample how to make a GUI for simple context variable.
	 * 
	 * * Make a copy * Change as needed * Add the component in Add
	 * MiscView.refreshView();
	 * 
	 * * If you want to make it "clickable with history graph";
	 * 
	 * 
	 * @author pop
	 * 
	 */
	private class BatteryLevelItem extends InfoItem {

		public boolean available() {
			return (null != BatteryLevel.getInstance());
		}

		public void setupView() {
			if (textView == null)
				return;

			String info = BatteryLevel.getInstance().getCurrentMainData().toString() + "%";

			textView.setText(info);
			textView.setGravity(Gravity.CENTER);

			title.setText("Battery Level");
			summary.setText("[%]");

			onClickActivityClazz = BatteryLevelDetailInfoActivityMP.class;
		}
	}

	private class AccelerometerItem extends InfoItem {

		public boolean available() {
			return (null !=

			Accelerometer.getInstance() &&

			Accelerometer.getInstance().isSupported());
		}

		public void setupView() {
			if (textView == null)
				return;
			Accelerometer AI = Accelerometer.getInstance();
			String info = String.format("%.2f [m/s^3]\n%.1f/%.1f/%.1f [m/s^2]", AI.getCurrentMainData().toDouble(), AI
					.getCurrentData().get(Accelerometer.GRAVITY_X).toDouble(),
					AI.getCurrentData().get(Accelerometer.GRAVITY_Y).toDouble(),
					AI.getCurrentData().get(Accelerometer.GRAVITY_Z).toDouble());

			textView.setText(info);
			textView.setGravity(Gravity.CENTER);

			title.setText("Accelerometer");
			summary.setText("Sum/s, x/y/z");

			onClickActivityClazz = AccelerometerDetailInfoActivityMP.class;
		}
	}
	
	
	private class OrientationItem extends InfoItem {

		public boolean available() {
			return (null !=

			Orientation.getInstance() &&

			Orientation.getInstance().isSupported()) || prefs.getBoolean("allow_orientation_sensor", false) == false;
		}

		public void setupView() {
			if (textView == null)
				return;
			
			String info = "Disabled in <a href='amobisense.prefs.params://'>settings</a>\n";
			
			if (prefs.getBoolean("allow_orientation_sensor", false)) {
				Orientation AI = Orientation.getInstance();
				info = String.format("%.2f [\u00B0/s]<br>%.1f/%.1f/%.1f [\u00B0]", AI.getCurrentMainData().toDouble(), AI
						.getCurrentData().get(Orientation.AZIMUTH).toDouble(),
						AI.getCurrentData().get(Orientation.PITCH).toDouble(),
						AI.getCurrentData().get(Orientation.ROLL).toDouble());
				
				onClickActivityClazz = OrientationDetailInfoActivityMP.class;
			}else {
				onClickActivityClazz = EditPreferences.class;
			}

			textView.setText(Html.fromHtml(info));
			textView.setGravity(Gravity.CENTER);

			title.setText("Orientation");
			summary.setText("\u0394/s, azimuth/roll/pitch");
		}
	}

	private class ProximityItem extends InfoItem {

		public boolean available() {
			return (null !=

			Proximity.getInstance() &&

			Proximity.getInstance().isSupported());
		}

		public void setupView() {
			if (textView == null)
				return;
			Proximity AI = Proximity.getInstance();
			// String info = String.format("Proximity %.2f [cm]",
			// AI.getCurrentMainData().toDouble()
			// );

			String info;
			if (Proximity.getInstance().getCurrentMainData().toDouble() == Proximity.MAX_VALUE) {
				info = "FAR";
			} else if (Proximity.getInstance().getCurrentMainData().toDouble() < 0.001) {
				info = "CLOSE";
			} else {
				info = String.format("%.2f", Proximity.getInstance().getCurrentMainData().toDouble());
			}

			textView.setText(info);
			textView.setGravity(Gravity.CENTER);

			title.setText("Front Proximity");
			summary.setText("[cm] or CLOSE/FAR");

			// onClickActivityClazz = ProximityMP.class;
		}
	}

	private class LightItem extends InfoItem {

		public boolean available() {
			return (null != Light.getInstance() && Light.getInstance().isSupported());
		}

		public void setupView() {
			if (textView == null)
				return;
			Light AI = Light.getInstance();
			// String info = String.format("Proximity %.2f [cm]",
			// AI.getCurrentMainData().toDouble()
			// );

			String info;
			info = String.format("%.2f lux", Light.getInstance().getCurrentMainData().toDouble());

			textView.setText(info);
			textView.setGravity(Gravity.CENTER);

			title.setText("Light");
			summary.setText("lux");

			// onClickActivityClazz = ProximityMP.class;
		}
	}

	private class BatteryTempItem extends InfoItem {

		public boolean available() {
			return uid == SystemInfo.AID_ALL && batteryStats.hasTemp();
		}

		public void setupView() {
			if (textView == null)
				return;
			double celcius = BatteryStats.getInstance().getTemp();
			double farenheit = 32 + celcius * 9.0 / 5.0;
			textView.setText(String.format("%1$.1f \u00b0C\n(%2$.1f \u00b0F)", celcius, farenheit));
			textView.setGravity(Gravity.CENTER);

			title.setText("Battery Temperature");
			summary.setText("[\u00b0C] ([\u00b0F])");
		}
	}

	private class WifiItem extends InfoItem {
		public boolean available() {
			return (null != WifiContext.getInstance());
		}

		public void setupView() {
			if (textView == null)
				return;
			String wifiinfo = "" + WifiContext.getInstance().getCurrentMainData().toLong();

			textView.setText(wifiinfo);
			textView.setGravity(Gravity.CENTER);

			title.setText("Wifi's Around");
			if (WifiContext.getInstance().getScanResults() != null) {
				onClickActivityClazz = SeenWifiInfoActivityMP.class;
				summary.setText("Nr of Wifi's seen, click for details");
			} else {
				summary.setText("Wi-Fi subsystem is disabled:-(");
			}
		}
	}

	private class GSMCellItem extends InfoItem {
		public boolean available() {
			return (GSMCells.getInstance().isSupported());
		}

		public void setupView() {
			if (textView == null)
				return;
			String wifiinfo = GSMCells.getInstance().getMiscItemString();

			textView.setText(wifiinfo);
			textView.setGravity(Gravity.CENTER);

			title.setText("GSM Net Info");
			if (GSMCells.getInstance().getScanResults() != null) {
				onClickActivityClazz = GSMCellsMP.class;
				summary.setText("Mobile net info");
			} else {
				summary.setText("Can not access this data:-(");
			}
		}
	}

	private class CPUUSageItem extends InfoItem {

		public boolean available() {
			return (null != CPU.getInstanceOrNull());
		}

		public void setupView() {
			if (textView == null)
				return;
			CPU cpuInstance = CPU.getInstanceOrNull();
			String infoString = "CPU data not available";

			if (cpuInstance != null) {
				infoString = String.format("USR: %2.1f%%\nSYS: %02.1f%%", cpuInstance.getUsrPerc(),
						cpuInstance.getSysPerc());
			}

			textView.setText(infoString);
			textView.setGravity(Gravity.CENTER);
			onClickActivityClazz = CPUUsageDetailInfoActivityMP.class;

			title.setText("CPU Usage");
			summary.setText("CPU Usage");
		}
	}

	private class InternetConnectionItem extends InfoItem {
		public boolean available() {
			return (null != InternetConnection.getInstance());
		}

		public void setupView() {
			if (textView == null)
				return;
			String info = "NOT SET";
			switch ((int) InternetConnection.getInstance().getCurrentMainData().toLong()) {
			case InternetConnection.CONNECTION_TYPE_NOT_CONNECTED:
				info = "NO";
				break;
			case InternetConnection.CONNECTION_TYPE_WIFI:
				info = "YES, WiFi \n" + InternetConnection.getInstance().getUnanonymizedData().toString();
				break;
			case InternetConnection.CONNECTION_TYPE_MOBILE:
				info = "YES Mobile";
				break;
			}

			textView.setText(info);
			textView.setGravity(Gravity.CENTER);
			onClickActivityClazz = ConnectivityDetailInfoMP.class;
			title.setText("Connection");
			summary.setText("Are you now online?");
		}
	}
	
	private class ScreenInfoItem extends InfoItem {
		public boolean available() {
			return (null != ScreenOrintationAndBrightness.getInstance());
		}

		public void setupView() {
			if (textView == null)
				return;
			String info = "N/A";
			
			switch ((int) ScreenOrintationAndBrightness.getInstance().getCurrentMainData().toLong()) {
			case 1:
			case 3:
				info = "Portrait";
				break;
			case 2:
			case 4:
				info = "Landscape";
				break;
			default:
				info = "?";
				break;
			}
			
			info += "\n" + 
					" " + (ScreenOrintationAndBrightness.getInstance().getCurrentData().get(ScreenOrintationAndBrightness.BRIGHTNESS).toLong());

			textView.setText(info);
			textView.setGravity(Gravity.CENTER);
			//onClickActivityClazz = ConnectivityDetailInfoMP.class;
			title.setText("Screen");
			summary.setText("Orientation / Brightness [0-255]");
		}
	}
	
}


