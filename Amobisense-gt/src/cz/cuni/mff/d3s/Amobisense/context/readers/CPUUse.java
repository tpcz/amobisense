package cz.cuni.mff.d3s.Amobisense.context.readers;


import android.content.Context;
import edu.umich.PowerTutor.dataReaders.CPU;
import edu.umich.PowerTutor.phone.PhoneConstants;

public class CPUUse extends AbstractPeriodicReader{

	@SuppressWarnings("unused")
	private final String TAG = "CPUUse";
	
	private static final String CPUUSR = "CPU-USE-USR";
	
	CPU cpuInstance;
	
	public static CPUUse instance = null;


	public static CPUUse getInstance() {
		return instance;
	}
	
	public CPUUse(Context c, PhoneConstants phoneValues) {
		super(c, phoneValues, "CPU-USE");
		instance = this;
		addResultDataItem(CPUUSR);
		cpuInstance = CPU.getInstanceOrNull();
	}


	@Override
	public boolean isSupported(){
		return cpuInstance != null;
	}
	
	public void clearResources() {
		super.clearResources();
	}
		
	@Override
	public void performScan(long iter) {
		synchronized (this) {
			currdata.get(readerID).setValue(cpuInstance.getUsrPerc() + cpuInstance.getSysPerc());
			currdata.get(CPUUSR).setValue(cpuInstance.getUsrPerc());
		}
	}
}