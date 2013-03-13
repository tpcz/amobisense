package cz.cuni.mff.d3s.Amobisense.context.readers;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import edu.umich.PowerTutor.phone.PhoneConstants;

public class Accelerometer extends AbstractPeriodicReader implements SensorEventListener {

	@SuppressWarnings("unused")
	private final String TAG = "AccelerometerContext";
	
	private SensorManager sensorManager;
	private boolean sensorSupported = false;
	
	private double historyCollector;
	
	public static String GRAVITY_X = "ACCEL-GR-X";
	public static String GRAVITY_Y = "ACCEL-GR-Y";
	public static String GRAVITY_Z = "ACCEL-GR-Z";

	public static Accelerometer instance = null;


	public static Accelerometer getInstance() {
		return instance;
	}
	
	public Accelerometer(Context c, PhoneConstants phoneValues) {
		super(c, phoneValues, "ACCELEROMETER-ACTIVITY");
		instance = this;
		
		sensorManager = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
		loggingFloatPrecision = 4;
		
		if (sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)){
			sensorSupported = true;
			
			historyCollector = 0.0;
			addResultDataItem(GRAVITY_X);
			addResultDataItem(GRAVITY_Y);
			addResultDataItem(GRAVITY_Z);
			//this.scanRunner.start();
		}
	}

	
	public boolean isSupported(){
		return sensorSupported;
	}
	
	public void clearResources() {
		super.clearResources();
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// can be ignored...
	}

	float[] gravity = {0F, 0F,0F };
	float[] gravityCumulator = {0F, 0F,0F };
	int numEvents = 0;
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
			
			  // from android reference documentation
			  // In this example, alpha is calculated as t / (t + dT),
			  // where t is the low-pass filter's time-constant and
			  // dT is the event delivery rate.

			  final float alpha = 0.8F;

			  // Isolate the force of gravity with the low-pass filter.
			  gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
			  gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
			  gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

			  // Remove the gravity contribution with the high-pass filter.
			  float x = event.values[0] - gravity[0];
			  float y = event.values[1] - gravity[1];
			  float z = event.values[2] - gravity[2];

			 double delta = Math.sqrt(x*x + y*y + z*z);
			
			 historyCollector += delta;
			 gravityCumulator[0] += event.values[0];
			 gravityCumulator[1] += event.values[1];
			 gravityCumulator[2] += event.values[2];
			 numEvents ++;
		}

	}
	
	@Override
	public void performScan(long iter) {
		synchronized (this) {
			currdata.get(readerID).setValue(historyCollector);
			currdata.get(GRAVITY_X).setValue(gravityCumulator[0]/Math.max(numEvents,1));
			currdata.get(GRAVITY_Y).setValue(gravityCumulator[1]/Math.max(numEvents,1));
			currdata.get(GRAVITY_Z).setValue(gravityCumulator[2]/Math.max(numEvents,1));
			 
			gravityCumulator[0] = 0;
			gravityCumulator[1] = 0;
			gravityCumulator[2] = 0;
			numEvents = 0;
			historyCollector = 0;
		}
	}
}