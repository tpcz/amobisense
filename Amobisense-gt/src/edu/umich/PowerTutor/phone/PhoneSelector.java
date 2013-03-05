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

package edu.umich.PowerTutor.phone;

import java.util.List;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import cz.cuni.mff.d3s.Amobisense.context.readers.AbstractReader;
import cz.cuni.mff.d3s.Amobisense.context.readers.Accelerometer;
import cz.cuni.mff.d3s.Amobisense.context.readers.BatteryLevel;
import cz.cuni.mff.d3s.Amobisense.context.readers.BatteryTemperature;
import cz.cuni.mff.d3s.Amobisense.context.readers.CPUUse;
import cz.cuni.mff.d3s.Amobisense.context.readers.GSMCells;
import cz.cuni.mff.d3s.Amobisense.context.readers.InternetConnection;
import cz.cuni.mff.d3s.Amobisense.context.readers.WifiContext;
import edu.umich.PowerTutor.dataReaders.Audio;
import edu.umich.PowerTutor.dataReaders.Audio.AudioData;
import edu.umich.PowerTutor.dataReaders.CPU;
import edu.umich.PowerTutor.dataReaders.CPU.CpuData;
import edu.umich.PowerTutor.dataReaders.GPS;
import edu.umich.PowerTutor.dataReaders.GPS.GpsData;
import edu.umich.PowerTutor.dataReaders.LCD;
import edu.umich.PowerTutor.dataReaders.LCD.LcdData;
import edu.umich.PowerTutor.dataReaders.OLED;
import edu.umich.PowerTutor.dataReaders.OLED.OledData;
import edu.umich.PowerTutor.dataReaders.PowerComponent;
import edu.umich.PowerTutor.dataReaders.Sensors;
import edu.umich.PowerTutor.dataReaders.Sensors.SensorData;
import edu.umich.PowerTutor.dataReaders.Threeg;
import edu.umich.PowerTutor.dataReaders.Threeg.ThreegData;
import edu.umich.PowerTutor.dataReaders.Wifi;
import edu.umich.PowerTutor.dataReaders.Wifi.WifiData;
import edu.umich.PowerTutor.service.PowerData;
import edu.umich.PowerTutor.util.NotificationService;
import edu.umich.PowerTutor.util.SystemInfo;

public class PhoneSelector {
	private static final String TAG = "PhoneSelector";

	public static final int PHONE_UNKNOWN = 0;
	public static final int PHONE_DREAM = 1; /* G1 */
	public static final int PHONE_SAPPHIRE = 2; /* G2 */
	public static final int PHONE_PASSION = 3; /* Nexus One */

	/* A hard-coded list of phones that have OLED screens. */
	public static final String[] OLED_PHONES = { "bravo", "passion", "GT-I9000", "inc", "legend", "GT-I7500",
			"SPH-M900", "SGH-I897", "SGH-T959", "desirec", };

	/*
	 * This class is not supposed to be instantiated. Just use the static
	 * members.
	 */
	private PhoneSelector() {
	}

	public static boolean phoneSupported() {
		return getPhoneType() != PHONE_UNKNOWN;
	}

	public static boolean hasOled() {
		for (int i = 0; i < OLED_PHONES.length; i++) {
			if (Build.DEVICE.equals(OLED_PHONES[i])) {
				return true;
			}
		}
		return false;
	}

	public static int getPhoneType() {
		if (Build.DEVICE.startsWith("dream"))
			return PHONE_DREAM;
		if (Build.DEVICE.startsWith("sapphire"))
			return PHONE_SAPPHIRE;
		if (Build.DEVICE.startsWith("passion"))
			return PHONE_PASSION;
		return PHONE_UNKNOWN;
	}

	public static PhoneConstants getConstants(Context context) {
		switch (getPhoneType()) {
		case PHONE_DREAM:
			return new DreamConstants(context);
		case PHONE_SAPPHIRE:
			return new SapphireConstants(context);
		case PHONE_PASSION:
			return new PassionConstants(context);
		default:
			boolean oled = hasOled();
			Log.w(TAG, "Phone type not recognized (" + Build.DEVICE + "), using " + (oled ? "Passion" : "Dream")
					+ " constants");
			return oled ? new PassionConstants(context) : new DreamConstants(context);
		}
	}

	public static PhonePowerCalculator getCalculator(Context context) {
		switch (getPhoneType()) {
		case PHONE_DREAM:
			return new DreamPowerCalculator(context);
		case PHONE_SAPPHIRE:
			return new SapphirePowerCalculator(context);
		case PHONE_PASSION:
			return new PassionPowerCalculator(context);
		default:
			boolean oled = hasOled();
			Log.w(TAG, "Phone type not recognized (" + Build.DEVICE + "), using " + (oled ? "Passion" : "Dream")
					+ " calculator");
			return oled ? new PassionPowerCalculator(context) : new DreamPowerCalculator(context);
		}
	}

	/** Add all periodically scanning components... */
	public static void generateComponents(Context context, List<PowerComponent> components,
			List<PowerFunction> functions) {
		final PhoneConstants constants = getConstants(context);
		final PhonePowerCalculator calculator = getCalculator(context);

		// TODO: What about bluetooth?
		// TODO: LED light on the Nexus

		/* Add display component. */
		if (hasOled()) {
			components.add(new OLED(context, constants));
			functions.add(new PowerFunction() {
				public double calculate(PowerData data) {
					return calculator.getOledPower((OledData) data);
				}
			});
		} else {
			components.add(new LCD(context));
			functions.add(new PowerFunction() {
				public double calculate(PowerData data) {
					return calculator.getLcdPower((LcdData) data);
				}
			});
		}

		/* Add CPU component. */
		components.add(new CPU(constants));
		functions.add(new PowerFunction() {
			public double calculate(PowerData data) {
				return calculator.getCpuPower((CpuData) data);
			}
		});

		/* Add Wifi component. */

		String wifiInterface = SystemInfo.getInstance().getProperty("wifi.interface");
		if (wifiInterface != null && wifiInterface.length() != 0) {
			components.add(new Wifi(context, constants));
			functions.add(new PowerFunction() {
				public double calculate(PowerData data) {
					return calculator.getWifiPower((WifiData) data);
				}
			});
		}

		/* Add 3G component. */
		if (constants.threegInterface().length() != 0) {
			components.add(new Threeg(context, constants));
			functions.add(new PowerFunction() {
				public double calculate(PowerData data) {
					return calculator.getThreeGPower((ThreegData) data);
				}
			});
		}

		/* Add GPS component. */
		components.add(new GPS(context, constants));
		functions.add(new PowerFunction() {
			public double calculate(PowerData data) {
				return calculator.getGpsPower((GpsData) data);
			}
		});

		/* Add Audio component. */
		components.add(new Audio(context));
		functions.add(new PowerFunction() {
			public double calculate(PowerData data) {
				return calculator.getAudioPower((AudioData) data);
			}
		});

		/* Add Sensors component if available. */
		if (NotificationService.available()) {
			components.add(new Sensors(context));
			functions.add(new PowerFunction() {
				public double calculate(PowerData data) {
					return calculator.getSensorPower((SensorData) data);
				}
			});
		}
	}

	/** Add all context readers (waiting typically waiting for broadcasts)... */
	public static void generateContextReaders(Context context, List<AbstractReader> contextReaders,
			List<PowerFunction> functions) {

		String wifiInterface = SystemInfo.getInstance().getProperty("wifi.interface");
		final PhoneConstants constants = getConstants(context);

		contextReaders.add(new BatteryLevel(context, constants));
		contextReaders.add(new BatteryTemperature(context, constants));
		contextReaders.add(new CPUUse(context, constants));
	
		if (wifiInterface != null && wifiInterface.length() != 0) {
			contextReaders.add(new WifiContext(context, constants));
		}

		Accelerometer amcr = new Accelerometer(context, constants);
		if (amcr.isSupported()) {
			contextReaders.add(amcr);
		}

		contextReaders.add(new InternetConnection(context, constants));
		
		GSMCells gsmc = new GSMCells(context, constants);
		if (gsmc.isSupported()) {
			contextReaders.add(gsmc);
		}
	}
}
