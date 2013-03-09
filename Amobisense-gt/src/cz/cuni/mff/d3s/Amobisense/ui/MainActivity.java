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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import cz.cuni.mff.d3s.Amobisense.R;
import cz.cuni.mff.d3s.Amobisense.service.MainBackgroundService;
import edu.umich.PowerTutor.phone.PhoneSelector;
import edu.umich.PowerTutor.service.DataCollector;
import edu.umich.PowerTutor.service.ICounterService;
import edu.umich.PowerTutor.ui.Help;
import edu.umich.PowerTutor.ui.PowerTabs;
import edu.umich.PowerTutor.ui.PowerTop;

/** The main view activity for PowerTutor */
public class MainActivity extends Activity {
	@SuppressWarnings("unused")
	private static final String TAG = "MainActivity";

	public static final String CURRENT_VERSION = "1.2"; // Don't change this...

	public static final String SERVER_IP = "spidermonkey.eecs.umich.edu";
	public static final int SERVER_PORT = 5204;

	private SharedPreferences prefs; 
	private Intent serviceIntent;
	private ICounterService counterService;
	private CounterServiceConnection conn;

	private Button serviceStartButton;
//	private Button appViewerButton;
//	private Button sysEnergyViewerButton;
//	private Button sysInformationButton;
//	private Button helpButton;
	public static final int MESSAGE_SUCCESS = 1;
	public static int MESSAGE_FAIL = 0;
	
	private TextView welcomeText;

	private static MainActivity instance = null;

	// private LogWebUploader logUploader = null;

	public static MainActivity getInstance() {
		return instance;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		instance = this;
		super.onCreate(savedInstanceState);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		serviceIntent = new Intent(this, MainBackgroundService.class);
		conn = new CounterServiceConnection();

		setContentView(R.layout.main);
		ArrayAdapter<?> adapterxaxis = ArrayAdapter.createFromResource(this, R.array.xaxis, android.R.layout.simple_spinner_item);
		adapterxaxis.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		serviceStartButton = (Button) findViewById(R.id.servicestartbutton);
		welcomeText = (TextView) findViewById(R.id.welcometext);
		//welcomeText.setPadding(10, 0, 10, 0);
		
		//appViewerButton = (Button) findViewById(R.id.appviewerbutton);
		//sysEnergyViewerButton = (Button) findViewById(R.id.sysenergyviewerbutton);
		//sysInformationButton = (Button) findViewById(R.id.sysviewerbutton);
		//helpButton = (Button) findViewById(R.id.helpbutton);

		serviceStartButton.setOnClickListener(serviceStartButtonListener);
		//sysEnergyViewerButton.setOnClickListener(sysEnergyViewerButtonListener);
		//appViewerButton.setOnClickListener(appViewerButtonListener);
		//sysInformationButton.setOnClickListener(sysInformationButtonListener);
		//helpButton.setOnClickListener(helpButtonListener);

		
		if (counterService != null) {
			serviceStartButton.setText("Stop AMobiSense");
			//serviceStartButton.setBackgroundResource(R.drawable.start_button_off);
			//appViewerButton.setEnabled(true);
			//sysInformationButton.setEnabled(true);
			//sysEnergyViewerButton.setEnabled(true);
			setRunningText();
			
		} else {
			serviceStartButton.setText("Start AMobiSense!");
			//serviceStartButton.setBackgroundResource(R.drawable.start_button_on);
			//appViewerButton.setEnabled(false);
			//sysEnergyViewerButton.setEnabled(false);
			//sysInformationButton.setEnabled(false);
			setStoppedText();
		}
	}
	
	private void setRunningText() {
		welcomeText.setText(Html.fromHtml(
				"<b>Power Use Information</b><br>" +
				"- <a href='amobisense.powertop://'>Per Applicaton</a> <br> " +
				"- <a href='amobisense.powertabs://'>Per Hardware</a><br> " +
				"- <a href='amobisense.powerpie://pie'>HW Share</a><br>" +
				
				"<b>Context Information</b><br>" +
				"- <a href='amobisense.context.overview://'>Context overview graphs</a><br>" +
				"- <a href='amobisense.context.misc://'>Detail information</a>" +
				//", e.g.," +
				//"<a href='amobisense.context.wifi://'>wifi's,</a> " +
				//"<a href='amobisense.context.connection://'>net connection</a>, " +
				//"<a href='amobisense.context.accelerometer://'>accelerometer</a> or " +
				//"<a href='amobisense.context.gsm://'>gsm cell information</a> " +
				"<br>" +
				"<br>" + 
				"You can have a look in <a href='amobisense.help://'> help & about</a> section for further details, and you can specify " +
				"your <a href='amobisense.prefs.personal://'>personal information</a> to help us understand better the data or " +
				"configure <a href='amobisense.prefs.params://'> parameters</a> (e.g. deny sending traces to our team). " +
				"Menu in this activity allows you to store current traces to SD card. " +
				"For more information see <a href='https://github.com/tpcz/amobisense/wiki'>Amobisense web</a>" +
				""));
		welcomeText.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	private void setStoppedText() {
		welcomeText.setText(Html.fromHtml(	
				
				"Start me to see:<br><b>- Power Viewer</b> showing applicaton power use, device subsystems power use" +
				" history and device susbsystems energy share<br>" +
				"<b>- Context Viewer</b> showing various information, as eq. WiFis around , Accelerometer activty, Cell-id, LAC and others. " +
				"<br>" +
				"<br>" + 
			
				"You can have a look in <a href='amobisense.help://'> help & about</a> section for further details, and you can specify " +
				"your <a href='amobisense.prefs.personal://'>personal information</a> to help us understand better the data or " +
				"configure <a href='amobisense.prefs.params://'> parameters</a> (e.g. deny sending traces to our team). " +
				"Menu in this activity allows you to store current traces to SD card. " +
				"For more information see <a href='https://github.com/tpcz/amobisense/wiki'>Amobisense web</a>" +
			 
				""));
		
	   welcomeText.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private void putDefaultValuesToPrefs() {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = prefs.edit();
		editor.putString("upload_url", "http://perun.ms.mff.cuni.cz:8000/upload");
		editor.putBoolean("anonymizeGSM", true);
		editor.putBoolean("anonymizeWIFI", true);
		editor.putLong("run_nr", 1);
		editor.commit();
	}

	private void incrementRunNumber() {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		long run_nr = prefs.getLong("run_nr", 1) + 1; 
		Editor editor = prefs.edit();
		editor.putLong("run_nr", run_nr);
		editor.commit();
	}

	@Override
	public void onResume() {
		super.onResume();
		instance = this;
		getApplicationContext().bindService(serviceIntent, conn, 0);
		if (prefs.getBoolean("firstRun", true)) {
			
			showDialog(DIALOG_TOS);
			
			putDefaultValuesToPrefs(); 
		} else {
			incrementRunNumber();
		}
		Intent startingIntent = getIntent();
		if (startingIntent.getBooleanExtra("isFromIcon", false)) {
			// Intent copyIntent = (Intent)getIntent().clone();
			// copyIntent.putExtra("isFromIcon", false);
			// setIntent(copyIntent);
			// Intent intent = new Intent(this, PowerTabs.class);
			// startActivity(intent);
		}
		
		welcomeText = (TextView) findViewById(R.id.welcometext);
		//welcomeText.setPadding(10, 0, 10, 0);
		
		

       // setLinkToActivity("device subsystems power use", PowerTabs.class);
       // setLinkToActivity("susbsystems energy share", PowerTabs.class);
       // setLinkToActivity("power share", PowerTabs.class);
       // setLinkToActivity("applicaton power use", PowerTop.class);
       // setLinkToActivity("context information", ContextInfoTabs.class);
	}

	
	
	 public void setLinkToActivity(String text, @SuppressWarnings("rawtypes") final Class activityClass) {
		 
		 	clickify(welcomeText, text, new ClickSpan.OnClickListener() {
	            @Override
	            public void onClick() {
	                //Toast.makeText(MainActivity.this, "Whale was clicked!", Toast.LENGTH_SHORT).show();
	            	startActivity(new Intent(MainActivity.this, activityClass));
	            }
	        });
	 }
	
	 public static void clickify(TextView view, final String clickableText,  final ClickSpan.OnClickListener listener) {
	        CharSequence text = view.getText();
	        String string = text.toString();
	        ClickSpan span = new ClickSpan(listener);

	        int start = string.indexOf(clickableText);
	        int end = start + clickableText.length();
	        if (start == -1) return;

	        if (text instanceof Spannable) {
	            ((Spannable)text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
	        } else {
	            SpannableString s = SpannableString.valueOf(text);
	            s.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
	            view.setText(s);
	        }

	        MovementMethod m = view.getMovementMethod();
	        if ((m == null) || !(m instanceof LinkMovementMethod)) {
	            view.setMovementMethod(LinkMovementMethod.getInstance());
	        }
	}
	



	@Override
	public void onPause() {
		super.onPause();
		getApplicationContext().unbindService(conn);
	}

	private static final int MENU_PREFERENCES = 0;
	private static final int MENU_SAVE_POWER_LOG = 1;
	private static final int MENU_SAVE_CONTEXT_LOG = 2;
	private static final int MENU_SHOW_CURRENT_SETTINGS = 3;
	// private static final int MENU_UPLOAD_CURRENT_CONTEXT_LOG = 4;
	private static final int DIALOG_START_SENDING = 0;
	private static final int DIALOG_STOP_SENDING = 1;
	private static final int DIALOG_TOS = 2;
	private static final int DIALOG_RUNNING_ON_STARTUP = 3;
	private static final int DIALOG_NOT_RUNNING_ON_STARTUP = 4;
	private static final int DIALOG_UNKNOWN_PHONE = 5;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_PREFERENCES, 0, "Edit Options");
		// menu.add(0, MENU_UPLOAD_CURRENT_CONTEXT_LOG, 0,
		// "Upload Context Log");
		menu.add(0, MENU_SAVE_POWER_LOG, 0, "Save power log");
		menu.add(0, MENU_SHOW_CURRENT_SETTINGS, 0, "Edit Personal Info");
		menu.add(0, MENU_SAVE_CONTEXT_LOG, 0, "Save context log");

		return true;
	}

	private void savePowerLog() {
		if (DataCollector.getInstance() != null) {
			DataCollector.getInstance().savePowerLogFile();
		} else {
			Message statusMsg = new Message();
			statusMsg.arg1 = MainActivity.MESSAGE_FAIL;
			statusMsg.obj = "not running..";
			MainActivity.makeToastHandler.sendMessage(statusMsg);
		}
	}

	private void saveContextLog() {
		if (DataCollector.getInstance() != null) {
			DataCollector.getInstance().saveContextLogFile();
		} else {
			Message statusMsg = new Message();
			statusMsg.arg1 = MainActivity.MESSAGE_FAIL; 
			statusMsg.obj = "not running..";
			MainActivity.makeToastHandler.sendMessage(statusMsg);
		}
	}

	public static Handler makeToastHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			if (instance != null) {
				String tmess = "";
				if (msg.obj != null) {
					tmess =  msg.obj.toString();
				}
				if (msg.arg1 == MESSAGE_FAIL) {
					Toast.makeText(instance, "FAILED :-( " + tmess, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(instance, "OK " + tmess, Toast.LENGTH_SHORT).show();
				}
			}
		}
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PREFERENCES:
			startActivity(new Intent(this, EditPreferences.class));
			return true;
		case MENU_SHOW_CURRENT_SETTINGS:
			startActivity(new Intent(this, EditPersonalInfo.class));
			return true;
		case MENU_SAVE_POWER_LOG:
			savePowerLog();
			return true;
		case MENU_SAVE_CONTEXT_LOG:
			saveContextLog();
			return true;
			// case MENU_UPLOAD_CURRENT_CONTEXT_LOG:
			// if (logUploader == null) {logUploader = new
			// LogWebUploader(this);}
			// if (DataCollector.getInstance() != null) {
			// logUploader.enqueueForUpload(getFileStreamPath(DataCollector.getInstance().getCurrentContextLogFileName()).getAbsolutePath(),
			// "ContextLog", false, false);
			// }
			// return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/** This function includes all the dialog constructor */
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_TOS:
			builder.setMessage(R.string.term).setCancelable(false)
					.setPositiveButton("Agree", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							prefs.edit().putBoolean("firstRun", false).putBoolean("runOnStartup", true)
									.putBoolean("sendPermission", true).commit();
							dialog.dismiss();
							if (PhoneSelector.getPhoneType() == PhoneSelector.PHONE_UNKNOWN) {
								showDialog(DIALOG_UNKNOWN_PHONE);
							}						}
					}).setNegativeButton("Do not agree", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							prefs.edit().putBoolean("firstRun", true).commit();
							finish();
						}
					});
			return builder.create();
		case DIALOG_STOP_SENDING:
			builder.setMessage(R.string.stop_sending_text).setCancelable(true)
					.setPositiveButton("Stop", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							prefs.edit().putBoolean("sendPermission", false).commit();
							dialog.dismiss();
						}
					}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		case DIALOG_START_SENDING:
			builder.setMessage(R.string.start_sending_text).setCancelable(true)
					.setPositiveButton("Start", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							prefs.edit().putBoolean("sendPermission", true).commit();
							dialog.dismiss();
						}
					}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		case DIALOG_RUNNING_ON_STARTUP:
			builder.setMessage(R.string.running_on_startup).setCancelable(true).setNeutralButton("Ok", null);
			return builder.create();
		case DIALOG_NOT_RUNNING_ON_STARTUP:
			builder.setMessage(R.string.not_running_on_startup).setCancelable(true).setNeutralButton("Ok", null);
			return builder.create();
		case DIALOG_UNKNOWN_PHONE:
			builder.setMessage(R.string.unknown_phone).setCancelable(false)
					.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
							//showDialog(DIALOG_TOS);
						}
					});
			return builder.create();

		}
		return null;
	}

	@SuppressWarnings("unused")
	private Button.OnClickListener appViewerButtonListener = new Button.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(v.getContext(), PowerTop.class);
			startActivityForResult(intent, 0);
		}
	};

	@SuppressWarnings("unused")
	private Button.OnClickListener sysEnergyViewerButtonListener = new Button.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(v.getContext(), PowerTabs.class);
			startActivityForResult(intent, 0);
		}
	};

	@SuppressWarnings("unused")
	private Button.OnClickListener sysInformationButtonListener = new Button.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(v.getContext(), ContextInfoTabs.class);
			startActivityForResult(intent, 0);
		}
	};

	private Button.OnClickListener serviceStartButtonListener = new Button.OnClickListener() {
		public void onClick(View v) {
			serviceStartButton.setEnabled(false);
			if (counterService != null) {
				stopService(serviceIntent);
			} else {
				if (conn == null) {
					Toast.makeText(MainActivity.this, "Profiler failed to start", Toast.LENGTH_SHORT).show();
				} else {
					startService(serviceIntent);
				}
			}
		}
	};

	private class CounterServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className, IBinder boundService) {
			counterService = ICounterService.Stub.asInterface((IBinder) boundService);
			serviceStartButton.setText("Stop AMobiSense!");
			//serviceStartButton.setBackgroundResource(R.drawable.start_button_off);
			serviceStartButton.setEnabled(true);
			setRunningText();
			startService(serviceIntent); 
			 
			//appViewerButton.setEnabled(true);
			//sysInformationButton.setEnabled(true);
			//sysEnergyViewerButton.setEnabled(true);
		}
 
		public void onServiceDisconnected(ComponentName className) {
			counterService = null;
			getApplicationContext().unbindService(conn);
			getApplicationContext().bindService(serviceIntent, conn, 0);

			Toast.makeText(MainActivity.this, "Stopped", Toast.LENGTH_SHORT).show();
			serviceStartButton.setText("Start AMobiSense!");
			
			serviceStartButton.setCompoundDrawablePadding(0);
			//serviceStartButton.setBackgroundResource(R.drawable.start_button_on);
			
			serviceStartButton.setEnabled(true);
			setStoppedText();
	
			//appViewerButton.setEnabled(false);
			//sysEnergyViewerButton.setEnabled(false);
			//sysInformationButton.setEnabled(false);
		}
	}
 
	@SuppressWarnings("unused")
	private Button.OnClickListener helpButtonListener = new Button.OnClickListener() {
		public void onClick(View v) {
			Intent myIntent = new Intent(v.getContext(), Help.class);
			startActivityForResult(myIntent, 0);
		}
	};
}
