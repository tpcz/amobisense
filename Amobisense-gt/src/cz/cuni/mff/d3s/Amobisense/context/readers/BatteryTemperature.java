package cz.cuni.mff.d3s.Amobisense.context.readers;


import android.content.Context;
import edu.umich.PowerTutor.phone.PhoneConstants;
import edu.umich.PowerTutor.util.BatteryStats;

public class BatteryTemperature extends AbstractPeriodicReader{

	@SuppressWarnings("unused")
	private final String TAG = "BatteryTemperature";
	
	private BatteryStats batteryStats;
	
	public static BatteryTemperature instance = null;


	public static BatteryTemperature getInstance() {
		return instance;
	}
	
	public BatteryTemperature(Context c, PhoneConstants phoneValues) {
		super(c, phoneValues, "BATTERY-TEMP");
		instance = this;
		batteryStats = BatteryStats.getInstance();
	}


	@Override
	public boolean isSupported(){
		return batteryStats != null;
	}
	
	public void clearResources() {
		super.clearResources();
	}
		
	@Override
	public void performScan(long iter) {
		synchronized (this) {
			currdata.get(readerID).setValue(batteryStats.getTemp());
		}
	}
}