/*
Copyright (C) 2011 The University of Michigan,
modified by Tomas Pop, Charles University in Prague.

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

 */

package edu.umich.PowerTutor.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import cz.cuni.mff.d3s.Amobisense.Constants;
import cz.cuni.mff.d3s.Amobisense.archivation.LogWebUploader;
import cz.cuni.mff.d3s.Amobisense.context.HistoryHolder;
import cz.cuni.mff.d3s.Amobisense.context.readers.AbstractPeriodicReader;
import cz.cuni.mff.d3s.Amobisense.context.readers.AbstractReader;
import cz.cuni.mff.d3s.Amobisense.context.readers.IDataReader;
import cz.cuni.mff.d3s.Amobisense.utils.CryptoUtils;
import edu.umich.PowerTutor.dataReaders.OLED;
import edu.umich.PowerTutor.dataReaders.PowerComponent;
import edu.umich.PowerTutor.phone.PhoneConstants;
import edu.umich.PowerTutor.phone.PhoneSelector;
import edu.umich.PowerTutor.phone.PowerFunction;
import edu.umich.PowerTutor.util.BatteryStats;
import edu.umich.PowerTutor.util.Counter;
import edu.umich.PowerTutor.util.HistoryBuffer;
import edu.umich.PowerTutor.util.NotificationService;
import edu.umich.PowerTutor.util.SystemInfo;
import edu.umich.PowerTutor.widget.PowerWidget;

/**
 * This class is responsible for starting the individual power component loggers
 * (CPU, GPS, etc...) and collecting the information they generate. This
 * information is used both to write a log file that will be send back to
 * spidermoneky (or looked at by the user) and to implement the ICounterService
 * IPC interface.
 */
public class DataCollector implements Runnable {
	private static final String TAG = "DataCollector";

	/*
	 * A dictionary used to assist in compression of the log files. Strings that
	 * appear more frequently should be put towards the end of the dictionary.
	 * It is not critical that every string that be written to the log appear
	 * here.
	 */
	private static final String DEFLATE_DICTIONARY = "onoffidleoff-hookringinglowairplane-modebatteryedgeGPRS3Gunknown"
			+ "in-serviceemergency-onlyout-of-servicepower-offdisconnectedconnecting"
			+ "associateconnectedsuspendedphone-callservicenetworkbegin.0123456789"
			+ "GPSAudioWifi3GLCDCPU-power ";

	public static final int ALL_COMPONENTS = -1;
	public static final int ITERATION_INTERVAL_SCALE = 1000; // 1 second
	public static final int ITERATION_INTERVAL = 1 * ITERATION_INTERVAL_SCALE; // 1
	public static final int HISTORY_UPDATE_INTERVAL = ITERATION_INTERVAL;
	
																				// second
	//public static final int CONTEXT_LOG_WRITE_NRITERATION = 10 / (ITERATION_INTERVAL / ITERATION_INTERVAL_SCALE); // 10
																													// second
	
	public static final int CONTEXT_LOG_UPLOAD_TRY_NRITERATION_SEC = 30 / (ITERATION_INTERVAL / ITERATION_INTERVAL_SCALE);
	
	public static final int POWER_LOG_UPLOAD_TRY_NRITERATION_SEC = CONTEXT_LOG_UPLOAD_TRY_NRITERATION_SEC;
	
	public static final int CONTEXT_LOG = 1;
	public static final int POWER_LOG = 2;
	public static final int ALL_LOGS = POWER_LOG | CONTEXT_LOG;

	public static final boolean DELETE_WHEN_UPLOADED = true;
	public static final boolean DONT_DELETE_WHEN_UPLOADED = false;
	
	public static final boolean CONTEXT_LOG_WRITE_STRATEGY = AbstractReader.WRITE_ONLY_ON_CHANGE;
	

	private MainBackgroundService context;
	private SharedPreferences prefs;

	private Vector<PowerComponent> powerComponents;
	private Vector<AbstractReader> contextReaders;
	//private Map<String, ContextStateWatch> contextReadersMap;
	private Vector<PowerFunction> powerFunctions;
	private Vector<HistoryBuffer> powerHistories;
	private Vector<HistoryHolder> contextHistories;
	private Map<Integer, String> uidAppIds;

	// Miscellaneous data.
	private HistoryBuffer oledScoreHistory;

	private Object powerLogFileWriteLock = new Object();
	private Object contextLogFileWriteLock = new Object();
	private LogWebUploader logUploader;
	private OutputStreamWriter powerLogStream;
	private OutputStreamWriter contextLogStream;
	private DeflaterOutputStream deflateStream;

	private Object iterationLock = new Object();
	private long lastWrittenIteration;

	private boolean firstPowerLogIteration = true;
	private boolean firstContextLogIteration = true;
	private long beginTime;
	private IterationData[] dataTemp;
	private int totalPower = 0;
	private int oledId = -1;
	private int numComponents;
	private double lastCurrent = -1;
	private long[] memInfo = new long[4];

	// TODO - more elegant - make from log object, holding these information...
	public String contextLogFileName = "ContextTrace.log";
	public static int contextLogFileNameRotationCounter = 1;
	public static long contextLogAlreadyUploadedNr = 0;

	public String powerLogFileName = "PowerTrace.log";
	
	public static int powerLogFileNameRotationCounter;
	public static long powerLogAlreadyUploadedNr = 0;

	private static DataCollector instance = null;
	private PhoneConstants phoneConstants;

	/** Constructor */
	public DataCollector(MainBackgroundService context) {
		this.context = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		powerComponents = new Vector<PowerComponent>();
		contextReaders = new Vector<AbstractReader>();
		powerFunctions = new Vector<PowerFunction>();
		contextHistories = new Vector<HistoryHolder>();
		uidAppIds = new HashMap<Integer, String>();
		instance = this;
		phoneConstants = PhoneSelector.getConstants(context);
 
		/* add all periodically scanning components */
		PhoneSelector.generateComponents(context, powerComponents,
				powerFunctions);

		powerHistories = new Vector<HistoryBuffer>();
		for (int i = 0; i < powerComponents.size(); i++) {
			powerHistories.add(new HistoryBuffer(300));
		}

		/* add all broadcasted components */
		PhoneSelector.generateContextReaders(context, contextReaders, powerFunctions);

		// just register it here - more data values will be produced
		// and historyholders will register themselfs!
		

		/* each context reader can produce more data vectors */
		// for(int i = 0; i < contextReaders.size(); i++) {
		// for (int j = 0; j < contextReaders.get(i).getContextVectorLength(); j
		// ++) {
		// contextHistories.add(new HistoryHolder(60, HISTORY_TYPE.INT));
		// }
		// }

		oledScoreHistory = new HistoryBuffer(0);

		/* Initialize log uploader */
		logUploader = new LogWebUploader(context);
		// set them to 0, rotate logs will increase it to 1.
		powerLogFileNameRotationCounter = 0;
		contextLogFileNameRotationCounter = 0;
		rotateLogs(true, ALL_LOGS);
	}

	public static DataCollector getInstance() {
		return instance;
	}

	// O(n), should not be called too many times..
	public IDataReader getContextWatchReference(String id) {

		for (int i = 0; i < this.contextReaders.size(); i++) {
			if (contextReaders.get(i).getReaderName().equals(id)) {
				return contextReaders.get(i);
			}
		}

		return null;
	}

	/** Main Loop, keeps updating the power profile */
	public void run() {

		

		beginTime = SystemClock.elapsedRealtime();
		oledId = getOLEDiD();

		initPowerComponents();
		initContextReaders();

		/*
		 * THE MAIN LOOP Indefinitely collect data on each of the power
		 * components.
		 */
		Log.i(TAG, "Enterin the main loop");
		for (long iter = -1; !Thread.interrupted();) {
			Log.i(TAG, "Iteration " + iter);

			numComponents = powerComponents.size();
			long curTime = SystemClock.elapsedRealtime();
			/*
			 * Compute the next iteration that we can make the ending of. We
			 * wait for the end of the iteration so that the components had a
			 * chance to collect data already. Sleep until next iteration
			 */
			iter = (long) Math.max(iter + 1, (curTime - beginTime) / ITERATION_INTERVAL);

			try {
				Thread.sleep(beginTime + (iter + 1) * ITERATION_INTERVAL - curTime);
			} catch (InterruptedException e) {
				break;
			}

			/* gets Values for all components and context readers */
			getCurrentValues(iter);
			
			/* update UI */
			if (iter % 15 == 14) {
				updateIcon(iter, phoneConstants);
			}
			if (iter % 60 == 0) {
				updateWidget(iter);
			}

			/* Write logs */
			
			/* there is no need to synchronized, values are updated only from this thread 
			 * and files are protected by extra lock! 
			 */
			writePowerLog(iter, dataTemp, totalPower);
			writeContextLog(iter);

			synchronized (iterationLock) {
				lastWrittenIteration = iter;
			}

		} // main loop

		Log.i(TAG, "After main loop...");

		finalizeAfterMainLoop();

	}

	public String getCurrentContextLogFileName() {
		synchronized (contextLogFileName) {
			return contextLogFileName + "-" + contextLogFileNameRotationCounter;
		}
	}

	public String getCurrentPowerLogFileName() {
		synchronized (powerLogFileWriteLock) {
			return powerLogFileName + "-" + powerLogFileNameRotationCounter;
		}
	}
	
	/** Open the log file if possible and close the old one in order to allow for upload. */
	private void rotateLogs(boolean init, int which) {
		//TODO if init is true, try to look for. Or to do this in mayBe upload log??
				// existing files and upload them potentialy

		if (0 != (which & POWER_LOG)) {
			try {
				Deflater deflater = new Deflater();
				deflater.setDictionary(DEFLATE_DICTIONARY.getBytes());
				synchronized (powerLogFileWriteLock) {
					powerLogFileNameRotationCounter += 1;
					String powerLogAbsolutPath = context.getFileStreamPath(getCurrentPowerLogFileName()).getAbsolutePath();

					deflateStream = new DeflaterOutputStream(new FileOutputStream(powerLogAbsolutPath));
					powerLogStream = new OutputStreamWriter(deflateStream);

				}
			} catch (IOException e) {
				powerLogStream = null;
				Log.e(TAG, "Failed to open power log file.  No log will be kept.");
			}
		}

		if (0 != (which & CONTEXT_LOG)) {
			try {
				// upload current...
				String contextLogAbsolutPath = context.getFileStreamPath(
						getCurrentContextLogFileName()).getAbsolutePath();
				// open new file, rotatet one...
				synchronized (contextLogFileWriteLock) {
					contextLogFileNameRotationCounter += 1;
					contextLogAbsolutPath = context.getFileStreamPath(
							getCurrentContextLogFileName()).getAbsolutePath();

					deflateStream = new DeflaterOutputStream(
							new FileOutputStream(contextLogAbsolutPath));
					contextLogStream = new OutputStreamWriter(deflateStream);
				}

			} catch (IOException e) {
				contextLogStream = null;
				Log.e(TAG,
						"Failed to open context log file.  No log will be kept.");
			}
		}
	}

	private void initPowerComponents() {
		int numComponents = powerComponents.size();
		
		for (int i = 0; i < numComponents; i++) {
			powerComponents.get(i).init(beginTime, ITERATION_INTERVAL);
			powerComponents.get(i).start();
		}
	}

	private void initContextReaders() {		
		int numComponents = contextReaders.size();
		
		for (int i = 0; i < numComponents; i++) {
			if (contextReaders.get(i) instanceof AbstractPeriodicReader) {
				((AbstractPeriodicReader)contextReaders.get(i)).init(beginTime, ITERATION_INTERVAL);
				((AbstractPeriodicReader)contextReaders.get(i)).start();
			}
		}
	}

	private int getOLEDiD() {
		int oledId = -1;
		int numComponents = powerComponents.size();
		for (int i = 0; i < numComponents; i++) {
			if ("OLED".equals(powerComponents.get(i).getComponentName())) {
				oledId = i;
				break;
			}
		}
		return oledId;
	}

	private void updateIcon(long iter, PhoneConstants phoneConstants) {

		/* Update the icon display every 15 iterations. */

		final double POLY_WEIGHT = 0.02;
		int count = 0;
		int[] history = getComponentHistory(5 * 60, -1, SystemInfo.AID_ALL, -1);

		double weightedAvgPower = 0;
		for (int i = history.length - 1; i >= 0; i--) {
			if (history[i] != 0) {
				count++;
				weightedAvgPower *= 1.0 - POLY_WEIGHT;
				weightedAvgPower += POLY_WEIGHT * history[i] / 1000.0;
			}
		}

		double avgPower = -1;
		if (count != 0) {
			avgPower = weightedAvgPower
					/ (1.0 - Math.pow(1.0 - POLY_WEIGHT, count));
		}
		avgPower *= 1000;

		context.updateNotification(
				(int) Math.min(8, 1 + 8 * avgPower / phoneConstants.maxPower()),
				avgPower);

	}

	private void updateWidget(long iter) {
		/* Update the widget. */

		PowerWidget.updateWidget(context, this);

	}

	private void getCurrentValues(long iter) {
		totalPower = 0;
		dataTemp = new IterationData[numComponents];
		
		
		// TODO!!

		for (int i = 0; i < numComponents; i++) {
			PowerComponent comp = powerComponents.get(i);
			IterationData data = comp.getData(iter);
			dataTemp[i] = data;
			if (data == null) {
				/*
				 * No data present for this timestamp. No power charged.
				 */
				continue;
			}

			SparseArray<PowerData> uidPower = data.getUidPowerData();
			for (int j = 0; j < uidPower.size(); j++) {
				int uid = uidPower.keyAt(j);
				PowerData powerData = uidPower.valueAt(j);
				int power = (int) powerFunctions.get(i).calculate(powerData);
				powerData.setCachedPower(power);
				powerHistories.get(i).add(uid, iter, power);
				if (uid == SystemInfo.AID_ALL) {
					totalPower += power;
				}

				if (i == oledId) {
					OLED.OledData oledData = (OLED.OledData) powerData;
					if (oledData.pixPower >= 0) {
						oledScoreHistory.add(uid, iter,
								(int) (1000 * oledData.pixPower));
					}
				}

			}
		}
	}

	// TODO un-implemented

	/**
	 * writes additional infomation as eg Phone type, user gender (if filled)
	 */

	private void writeGeneralInfo(OutputStreamWriter out) {
		try {
			// mark as a comment
			Calendar cal = new GregorianCalendar();
			Time time = new Time(Time.getCurrentTimezone());
			time.setToNow();
			long curUnixTime = time.toMillis(false);
			
			out.write("responder-id " + CryptoUtils.getEncryptedUserUID(context) + "\n");
			out.write("utime " + curUnixTime + "\n");
			out.write("iter-interval " + ITERATION_INTERVAL + "\n");
			out.write("local-time-offset " + (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) + "\n");
			out.write("device: " + getDeviceName() + "\n");
			out.write("log-version: " + Constants.CONTEXT_LOG_VERSION + "\n");
			out.write("amobisense-version: " + Constants.VERISON + "\n");
			out.write("write-only-on-change: " + CONTEXT_LOG_WRITE_STRATEGY + "\n");
			
			out.write("age " + prefs.getString("personalinfo_age", "0") +  "\n");
			out.write("gender " + prefs.getString("personalinfo_gender", "-") +  "\n");
			out.write("education " + prefs.getString("personalinfo_education", "-") +  "\n");
			out.write("job-position " + prefs.getString("personalinfo_job", "-") +  "\n\n");
			
		} catch (IOException e) {
			Log.w(TAG, "Failed to write header to context log file");
		}
	}
	
	public String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return capitalize(model);
		} else {
			return capitalize(manufacturer) + " " + model;
		}
	}

	private String capitalize(String s) {
		if (s == null || s.length() == 0) {
			return "";
		}
		char first = s.charAt(0);
		if (Character.isUpperCase(first)) {
			return s;
		} else {
			return Character.toUpperCase(first) + s.substring(1);
		}
	}

	public void flushContextLog() {
		synchronized (contextLogFileWriteLock) {
			if (contextLogStream != null)
				try {
					contextLogStream.flush();
				} catch (IOException e) {
					Log.w(TAG, "Can not flus context stream");
				}
		}
	}

	/** Write context information */
	private void writeContextLog(long iter) {
		/* Zkontrolovat, ze mam stejne readeru jako minule */
		/* Pokud Ano, OK, jen zapsat vektor */
		/* Pokud ne, zapsat novou hlavicku */
		/*
		 * rict vsem kontext readerum, aby vratili svuji hlavicku (vcetne
		 * subreaderu) zapsat hodnotu..
		 */

		

		synchronized (contextLogFileWriteLock) {
			//Log.i(TAG, "Writing context info");

			if (contextLogStream != null)
				try {
					if (firstContextLogIteration) {
						writeGeneralInfo(contextLogStream);
						firstContextLogIteration = false;
					}

					contextLogStream.write("begin " + iter + "\n");
					
					for (AbstractReader ar : contextReaders) {
						//Log.w(TAG, ar.getReaderName() + ": Going to Write to log..." );
						ar.writeLog(contextLogStream, iter,  CONTEXT_LOG_WRITE_STRATEGY);
					}
					
					contextLogStream.write("end " + iter + "\n");

				} catch (IOException e) {
					Log.e(TAG, "Failed to write to log file " + e.getMessage() + e.getClass().toString());
				}
		}

		if (((iter + 1) % CONTEXT_LOG_UPLOAD_TRY_NRITERATION_SEC) == 0
				&& prefs.getBoolean("sendPermission", true)) {
			mayBeUploadLogFile(contextLogFileWriteLock,
					getCurrentContextLogFileName(), contextLogStream,
					CONTEXT_LOG, true);
		}
	}

	private void mayBeUploadLogFile(Object lock, String fileName, OutputStreamWriter stream, int whichLog, boolean inflate) {
		synchronized (lock) {

			switch (whichLog) {
			case CONTEXT_LOG:
				if (contextLogAlreadyUploadedNr == contextLogFileNameRotationCounter) {
					return;
				}
				break;
			case POWER_LOG:
				if (powerLogAlreadyUploadedNr == powerLogFileNameRotationCounter) {
					return;
				}
				break;
			}

			if (logUploader.shouldUpload()) {

				String uploadFileNameBase = "";
				switch (whichLog) {
				case CONTEXT_LOG:

					uploadFileNameBase = "context-log";
					contextLogAlreadyUploadedNr = contextLogFileNameRotationCounter;
					//Log.i(TAG, "Going to upload context files, counter = " + contextLogFileNameRotationCounter);
					firstContextLogIteration = true;
					break;
				case POWER_LOG:

					uploadFileNameBase = "power-log";
					powerLogAlreadyUploadedNr = powerLogFileNameRotationCounter;
					//Log.i(TAG, "Going to upload power files, counter = " + powerLogFileNameRotationCounter);
					firstPowerLogIteration = true;
					break;
				}

				OutputStreamWriter tmpOutStreamWriter = stream;
				String tmpLogFileName = fileName;

				try {
					tmpOutStreamWriter.close();
				} catch (IOException e) {
					Log.w(TAG, "Failed to flush and close log stream");
				}

				// open new file
				rotateLogs(false, whichLog);

				// upload asyncronously old files

				logUploader.enqueueForUpload(
						context.getFileStreamPath(tmpLogFileName)
								.getAbsolutePath(), uploadFileNameBase,
						DELETE_WHEN_UPLOADED, inflate);

			}
		}
	}

	private void writePowerLog(long iter, IterationData[] dataTemp,
			long totalPower) {

		BatteryStats bst = BatteryStats.getInstance();
		SystemInfo sysInfo = SystemInfo.getInstance();
		PackageManager pm = context.getPackageManager();

		/* Update the uid set. */
		synchronized (powerLogFileWriteLock) {
			synchronized (uidAppIds) {
				for (int i = 0; i < numComponents; i++) {
					IterationData data = dataTemp[i];
					if (data == null) {
						continue;
					}
					SparseArray<PowerData> uidPower = data.getUidPowerData();
					for (int j = 0; j < uidPower.size(); j++) {
						int uid = uidPower.keyAt(j);
						if (uid < SystemInfo.AID_APP) {
							uidAppIds.put(uid, null);
						} else {
							/*
							 * We only want to update app names when logging so
							 * the associcate message gets written.
							 */
							String appId = uidAppIds.get(uid);
							String newAppId = sysInfo.getAppId(uid, pm);
							if (!firstPowerLogIteration
									&& powerLogStream != null
									&& (appId == null || !appId
											.equals(newAppId))) {
								try {
									powerLogStream.write("associate " + uid
											+ " " + newAppId + "\n");
								} catch (IOException e) {
									Log.w(TAG, "Failed to write to log file");
								}
							}
							uidAppIds.put(uid, newAppId);
						}
					}
				}
			}
		}

		if (bst.hasCurrent()) {
			double current = bst.getCurrent();
			if (current != lastCurrent) {
				writeToPowerLog("batt_current " + current + "\n");
				lastCurrent = current;
			}
		}
		if (iter % (5 * 60) == 0) {
			if (bst.hasTemp()) {
				writeToPowerLog("batt_temp " + bst.getTemp() + "\n");
			}
			if (bst.hasCharge()) {
				writeToPowerLog("batt_charge " + bst.getCharge() + "\n");
			}
		}
		if (iter % (30 * 60) == 0) {
			if (Settings.System.getInt(context.getContentResolver(),
					"screen_brightness_mode", 0) != 0) {
				writeToPowerLog("setting_brightness automatic\n");
			} else {
				int brightness = Settings.System.getInt(
						context.getContentResolver(),
						Settings.System.SCREEN_BRIGHTNESS, -1);
				if (brightness != -1) {
					writeToPowerLog("setting_brightness " + brightness + "\n");
				}
			}
			int timeout = Settings.System.getInt(context.getContentResolver(),
					Settings.System.SCREEN_OFF_TIMEOUT, -1);
			if (timeout != -1) {
				writeToPowerLog("setting_screen_timeout " + timeout + "\n");
			}
			String httpProxy = Settings.Secure.getString(
					context.getContentResolver(), Settings.Secure.HTTP_PROXY);
			if (httpProxy != null) {
				writeToPowerLog("setting_httpproxy " + httpProxy + "\n");
			}
		}

		/*
		 * Let's only grab memory information every 10 seconds to try to keep
		 * log file size down and the notice_data table size down.
		 */
		boolean hasMem = false;
		if (iter % 10 == 0) {
			hasMem = sysInfo.getMemInfo(memInfo);
		}

		synchronized (powerLogFileWriteLock) {
			if (powerLogStream != null)
				try {
					if (firstPowerLogIteration) {
						firstPowerLogIteration = false;
						powerLogStream.write("time "
								+ System.currentTimeMillis() + "\n");
						Calendar cal = new GregorianCalendar();
						powerLogStream.write("localtime_offset "
								+ (cal.get(Calendar.ZONE_OFFSET) + cal
										.get(Calendar.DST_OFFSET)) + "\n");
						powerLogStream.write("model "
								+ phoneConstants.modelName() + "\n");
						if (NotificationService.available()) {
							powerLogStream.write("notifications-active\n");
						}
						if (bst.hasFullCapacity()) {
							powerLogStream.write("batt_full_capacity "
									+ bst.getFullCapacity() + "\n");
						}
						synchronized (uidAppIds) {
							for (int uid : uidAppIds.keySet()) {
								if (uid < SystemInfo.AID_APP) {
									continue;
								}
								powerLogStream.write("associate " + uid + " "
										+ uidAppIds.get(uid) + "\n");
							}
						}
					}
					powerLogStream.write("begin " + iter + "\n");
					powerLogStream.write("total-power "
							+ (long) Math.round(totalPower) + '\n');
					if (hasMem) {
						powerLogStream.write("meminfo " + memInfo[0] + " "
								+ memInfo[1] + " " + memInfo[2] + " "
								+ memInfo[3] + "\n");
					}
					for (int i = 0; i < numComponents; i++) {
						IterationData data = dataTemp[i];
						if (data != null) {
							String name = powerComponents.get(i)
									.getComponentName();
							SparseArray<PowerData> uidData = data
									.getUidPowerData();
							for (int j = 0; j < uidData.size(); j++) {
								int uid = uidData.keyAt(j);
								PowerData powerData = uidData.valueAt(j);
								if (uid == SystemInfo.AID_ALL) {
									powerLogStream.write(name
											+ " "
											+ (long) Math.round(powerData
													.getCachedPower()) + "\n");
									powerData.writeLogDataInfo(powerLogStream);
								} else {
									powerLogStream.write(name
											+ "-"
											+ uid
											+ " "
											+ (long) Math.round(powerData
													.getCachedPower()) + "\n");
								}
							}
							data.recycle();
						}
					}
				} catch (IOException e) {
					Log.w(TAG, "Failed to write to log file");
				}

			if ((iter+1) % POWER_LOG_UPLOAD_TRY_NRITERATION_SEC == 0
					&& prefs.getBoolean("sendPermission", true)) {
				mayBeUploadLogFile(powerLogFileWriteLock,
						getCurrentPowerLogFileName(), powerLogStream,
						POWER_LOG, true);
			}

		}
	}

	/**
	 * Close the logstream so that everything gets flushed and written to file
	 * before we have to quit.
	 */
	private void closeLogFiles() {
		synchronized (powerLogFileWriteLock) {
			if (powerLogStream != null)
				try {
					powerLogStream.close();
				} catch (IOException e) {
					Log.w(TAG, "Failed to flush power log file on exit");
				}
		}

		synchronized (contextLogStream) {
			if (powerLogStream != null)
				try {
					contextLogStream.close();
				} catch (IOException e) {
					Log.w(TAG, "Failed to flush context log file on exit");
				}
		}
	}

	private void finalizeAfterMainLoop() {
		/* Blank the widget's display and turn off power button. */
		PowerWidget.updateWidgetDone(context);
		int numComponents = powerComponents.size();

		Log.i("UPLOADER THREAD", "CLEANING RESOURCES");

		/* Have all of the power component threads exit. */
		logUploader.interrupt();

		try {
			logUploader.join();
		} catch (InterruptedException e) {
		
		}

		Log.i("UPLOADER THREAD", "Uploader thread joined");

		for (int i = 0; i < numComponents; i++) {
			powerComponents.get(i).interrupt();
		}

		for (int i = 0; i < numComponents; i++) {
			try {
				powerComponents.get(i).join();
			} catch (InterruptedException e) {
			}
		}

		for (int i = 0; i < contextReaders.size(); i++) {
			contextReaders.get(i).clearResources();
		}

		closeLogFiles();
	}

	public void writePersonalInfo(OutputStreamWriter out) {
		// TODO Unimplemented
	}

	public void plug(boolean plugged) {
		logUploader.plug(plugged);
	}

	public void writeToPowerLog(String m) {
		synchronized (powerLogFileWriteLock) {
			if (powerLogStream != null)
				try {
					powerLogStream.write(m);
				} catch (IOException e) {
					Log.w(TAG, "Failed to write message to power log");
				}
		}
	}

	public String[] getComponents() {
		int components = powerComponents.size();
		String[] ret = new String[components];
		for (int i = 0; i < components; i++) {
			ret[i] = powerComponents.get(i).getComponentName();
		}
		return ret;
	}

	public int[] getComponentsMaxPower() {
		PhoneConstants constants = PhoneSelector.getConstants(context);
		int components = powerComponents.size();
		int[] ret = new int[components];
		for (int i = 0; i < components; i++) {
			ret[i] = (int) constants.getMaxPower(powerComponents.get(i)
					.getComponentName());
		}
		return ret;
	}

	public int getNoUidMask() {
		int components = powerComponents.size();
		int ret = 0;
		for (int i = 0; i < components; i++) {
			if (!powerComponents.get(i).hasUidInformation()) {
				ret |= 1 << i;
			}
		}
		return ret;
	}

	public int[] getComponentHistory(int count, int componentId, int uid,
			long iteration) {
		if (iteration == -1)
			synchronized (iterationLock) {
				iteration = lastWrittenIteration;
			}
		int components = powerComponents.size();
		if (componentId == ALL_COMPONENTS) {
			int[] result = new int[count];
			for (int i = 0; i < components; i++) {
				int[] comp = powerHistories.get(i).get(uid, iteration, count);
				for (int j = 0; j < count; j++) {
					result[j] += comp[j];
				}
			}
			return result;
		}
		if (componentId < 0 || components <= componentId)
			return null;
		return powerHistories.get(componentId).get(uid, iteration, count);
	}

	public long[] getTotals(int uid, int windowType) {
		int components = powerComponents.size();
		long[] ret = new long[components];
		for (int i = 0; i < components; i++) {
			ret[i] = powerHistories.get(i).getTotal(uid, windowType)
					* ITERATION_INTERVAL / 1000;
		}
		return ret;
	}

	public long getRuntime(int uid, int windowType) {
		long runningTime = 0;
		int components = powerComponents.size();
		for (int i = 0; i < components; i++) {
			long entries = powerHistories.get(i).getCount(uid, windowType);
			runningTime = entries > runningTime ? entries : runningTime;
		}
		return runningTime * ITERATION_INTERVAL / 1000;
	}

	public long[] getMeans(int uid, int windowType) {
		long[] ret = getTotals(uid, windowType);
		long runningTime = getRuntime(uid, windowType);
		runningTime = runningTime == 0 ? 1 : runningTime;
		for (int i = 0; i < ret.length; i++) {
			ret[i] /= runningTime;
		}
		return ret;
	}

	public UidInfo[] getUidInfo(int windowType, int ignoreMask) {
		long iteration;
		synchronized (iterationLock) {
			iteration = lastWrittenIteration;
		}
		int components = powerComponents.size();
		synchronized (uidAppIds) {
			int pos = 0;
			UidInfo[] result = new UidInfo[uidAppIds.size()];
			for (Integer uid : uidAppIds.keySet()) {
				UidInfo info = UidInfo.obtain();
				int currentPower = 0;
				for (int i = 0; i < components; i++) {
					if ((ignoreMask & 1 << i) == 0) {
						currentPower += powerHistories.get(i).get(uid,
								iteration, 1)[0];
					}
				}
				info.init(
						uid,
						currentPower,
						sumArray(getTotals(uid, windowType), ignoreMask)
								* ITERATION_INTERVAL / ITERATION_INTERVAL_SCALE,
						getRuntime(uid, windowType) * ITERATION_INTERVAL
								/ ITERATION_INTERVAL_SCALE);
				result[pos++] = info;
			}
			return result;
		}
	}

	private long sumArray(long[] A, int ignoreMask) {
		long ret = 0;
		for (int i = 0; i < A.length; i++) {
			if ((ignoreMask & 1 << i) == 0) {
				ret += A[i];
			}
		}
		return ret;
	}

	public long getUidExtra(String name, int uid) {
		if ("OLEDSCORE".equals(name)) {
			long entries = oledScoreHistory.getCount(uid, Counter.WINDOW_TOTAL);
			if (entries <= 0)
				return -2;
			double result = oledScoreHistory
					.getTotal(uid, Counter.WINDOW_TOTAL)
					/ ITERATION_INTERVAL_SCALE;
			result /= entries;
			PhoneConstants phoneConstants = PhoneSelector.getConstants(context);
			result *= 255 / (phoneConstants.getMaxPower("OLED") - phoneConstants
					.oledBasePower());
			return (long) Math.round(result * 100);
		}
		return -1;
	}

	public void registerContextHistory(HistoryHolder historyHolder) {
		contextHistories.add(historyHolder);
	}
}
