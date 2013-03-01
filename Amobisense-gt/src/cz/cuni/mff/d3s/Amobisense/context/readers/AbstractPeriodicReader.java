package cz.cuni.mff.d3s.Amobisense.context.readers;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import edu.umich.PowerTutor.phone.PhoneConstants;

public abstract class AbstractPeriodicReader extends AbstractReader implements IDataReader {
	
	private final String TAG = "PeriodicReader";
	
	
	/*
	 * Extending classes need to override the calculateIteration function. It
	 * should calculate the data point for the given component in a timely
	 * manner (under 1 second, longer times will cause data to be missed). The
	 * iteration parameter can be ignored in most cases.
	 */
	protected abstract void performScan(long iteration);

	/*
	 * Returns true if this component collects usage information per uid.
	 */
	public boolean hasUidInformation() {
		return false;
	}
	
	protected Thread scanRunner = new Thread() {
		@Override
		public void run(){
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
			for (long iter = 0; !Thread.interrupted();) {
				
				performScan(iter);
				
				if (interrupted()) {
					break;
				}

				/* Compute the next iteration that we can make the start of. */
				long curTime = SystemClock.elapsedRealtime();
				
				long previousIter = iter;
				iter = (long) Math.max(iter + 1, 1 + (curTime - beginTime) / iterationInterval);
				if (previousIter + 1 != iter) {
					Log.w(TAG, "[" + getReaderName() + "] Had to skip from iteration " + previousIter + " to " + iter);
				}
				
				updateHistory();
				
				/* Sleep until the next iteration completes. */
				try {
					sleep(beginTime + iter * iterationInterval - curTime);
				} catch (InterruptedException e) {
					break;
				}
			}
			AbstractPeriodicReader.this.clearResources();
		}
	};
	
	protected long beginTime;
	protected long iterationInterval;

	public AbstractPeriodicReader(Context c, PhoneConstants phoneValues,  String mainDataId) {
		super(c, phoneValues, mainDataId);
		scanRunner.setDaemon(true);
	}
	
	public final void start() {
		this.scanRunner.start();
	}
	
	/*
	 * This is called once before the it is set deamon.
	 */
	public void init(long beginTime, int iterationInterval) {
		this.beginTime = beginTime;
		this.iterationInterval = iterationInterval;
	}
	
	@Override
	public String getReaderType() {
		return AbstractEventReader.TYPE_PERIODIC;
	}
}
