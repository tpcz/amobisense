package cz.cuni.mff.d3s.Amobisense.context.readers;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import cz.cuni.mff.d3s.Amobisense.context.ContextData;
import cz.cuni.mff.d3s.Amobisense.context.ContextType;
import cz.cuni.mff.d3s.Amobisense.context.HistoryHolder;
import edu.umich.PowerTutor.phone.PhoneConstants;



public abstract class AbstractReader implements IDataReader{
	
	public static final String TYPE_PERIODIC = "PERIODIC";
	public static final String TYPE_BROADCAST = "BROADCAST";
	public static final String TYPE_PHONESTATE = "PHONESTATE";
	
	public static final boolean WRITE_ONLY_ON_CHANGE = true;
	public static final boolean WRITE_EACH_ITTERATION = true;
	
	public static final boolean DONT_SHOW_IN_LOG = false;
	public static final boolean SHOW_IN_LOG = true;
	
	protected int loggingFloatPrecision = 0;
	
	
	
	protected Handler handler = new Handler();	
	protected HashMap<String, HistoryHolder> history = new HashMap<String, HistoryHolder>();
	protected HashMap<String, ContextData> currdata = new HashMap<String, ContextData>();
	protected ArrayList<Pair<String, ContextData>> currdataOrder = new ArrayList<Pair<String, ContextData>>();
	protected HashMap<String, ContextData> prevdata = new HashMap<String, ContextData>();
	protected int historySize;
	protected Context c = null;
	protected PhoneConstants phoneValues = null;
	protected SharedPreferences prefs = null;
	
	/** should be shadow in subclasses! 
	 * 
	 * TAG is static and should be used only to write to log files.
	 * Use readerID for all other purposes!
	 * 
	 * */
	public static final String TAG = "AbstractReader";
	
	/** should be overridden in subclasses! */
	protected String readerID;
	
	
	public AbstractReader(Context c, PhoneConstants phoneValues,  String mainDataId) {
		this.c = c;
		this.phoneValues = phoneValues;
		// TODO check, that there is no other component with the same id.
		// in Datacollector.contextReaders.
		this.readerID = mainDataId;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(c);
		
		addResultDataItem(mainDataId);
		this.historySize = Integer.parseInt(prefs.getString("viewNumValues_s", "60"));
	}
	
	/** should be called only from constructor */
	final protected void addResultDataItem(String resultName) {
		ContextData cd = new ContextData();
		this.currdata.put(resultName, cd);
		this.currdataOrder.add(new Pair<String, ContextData>(resultName, cd));
	}
	
	
	@Override
	/**
	 * Check, whether this sensor is supported on device.
	 * Method MUST return correct values, after constructor was run.
	 */
	public abstract boolean isSupported();
	
	
	/**
	 * Called once, when the thread running this interface is asked to exit.
	 */
	public void clearResources() {
	
	}
	
	public long[] getLongHistoryValues(String Identifier) {
		if (history.containsKey(Identifier)) {
			return history.get(Identifier).getLHistory();
		} else {
			return null;
		}
	}
	
	public long[] getMainLongHistoryValues() {
		return this.getLongHistoryValues(readerID);
	}
	
	public double[] getMainDoubleHistoryValues() {
		return this.getDoubleHistoryValues(readerID);
	}
	
	public ContextData getCurrentMainData() {
		return this.currdata.get(readerID);
	}
	
	
	public double[] getDoubleHistoryValues(String Identifier) {
		if (history.containsKey(Identifier)) {
			return history.get(Identifier).getDHistory();
		} else {
			return null;
		}
	}	

	protected void updateHistory() {
		for (String key : currdata.keySet()) {
			// don't store history for string data, no graphs can be drawn
			if (currdata.get(key).dataType == ContextType.STRING) {
				continue;
			}
			synchronized (currdata) {
				synchronized (history) {
					if (!history.containsKey(key)) {
						history.put(key, new HistoryHolder(historySize, currdata.get(key).dataType, key));
					}
					if (getCurrentData().get(key).dataType == ContextType.LONG) {
						history.get(key).addPoint(getCurrentData().get(key).toLong());
					}
					else if (getCurrentData().get(key).dataType == ContextType.DOUBLE) {
						history.get(key).addPoint(getCurrentData().get(key).toDouble());
					} 
					else if (getCurrentData().get(key).dataType == ContextType.MIXED_LNUM_STRING) {
						history.get(key).addPoint(getCurrentData().get(key).toLong(), getCurrentData().get(key).toString());
					}
					else if (getCurrentData().get(key).dataType == ContextType.STRING) {
						history.get(key).addPoint(getCurrentData().get(key).toString());
					}
				}
			}
		}
	}

	public HashMap<String, ContextData> getCurrentData() {
		return currdata;
	}

	@Override
	public String getReaderName() {
		return readerID;
	}
	
	public HashMap<String, HistoryHolder> getHistory() {
		return history;
	}
	
	private boolean firstRun = true;
	
	
	public static boolean equalsStringBuilder(StringBuilder cur, StringBuilder prev){
		
		if (cur.length() != prev.length()) {
			return false;
		}
		
		for (int i = 0 ; i <cur.length(); i ++){
			if (cur.charAt(i) != prev.charAt(i)) {
				return false;
			}
		}
		
		return true;
	}
	
	
	private void updateKnownWrittenValues(){
		synchronized (this.currdata) {
			
			for (String key: currdata.keySet()) {
				
				if (prevdata.get(key) == null) {
					prevdata.put(key, new ContextData());
				}
				prevdata.get(key).setValue(currdata.get(key));
			}
		}
	}
	
	/** writes data in a orderred way */
	private void consequentWriteOnChangeStrategy(OutputStreamWriter logStream){
		synchronized (currdata) {
			for (Pair<String, ContextData> keyPair : currdataOrder) {
				if (0 != currdata.get(keyPair.first).compareTo(prevdata.get(keyPair.first))) {
					writeContextDataValue (logStream, keyPair.first, keyPair.second);
				}
			}
		}
	}
	
	private void writeAllValues(OutputStreamWriter logStream) {
		synchronized (currdata) {
			for (Pair<String, ContextData> keyPair : currdataOrder) {
					writeContextDataValue (logStream, keyPair.first, keyPair.second);
			}
		}
	}

	public void writeLog(OutputStreamWriter logStream, long iter, boolean writeOnlyOnChange) {
		
		//Log.w(TAG, getReaderName() + ": Writing to log..." );
		
		// write all values
		if (writeOnlyOnChange != WRITE_ONLY_ON_CHANGE) {
			writeAllValues(logStream);
		
		// write on change	
		} else {
			if (firstRun) {
				writeAllValues(logStream);
				firstRun = false;
				updateKnownWrittenValues();
				
			} else {
				consequentWriteOnChangeStrategy(logStream);	
				updateKnownWrittenValues();
			}
		}
	}
	
	// can be set in subclasses
	String[] intValuesMapping = null;
	StringBuilder writerStringbuilder = new StringBuilder();
	private void writeContextDataValue(OutputStreamWriter logStream, String key,  ContextData data) {
		try {
			writerStringbuilder.setLength(0);
			switch (data.dataType) {
			case LONG:
			case INT:
				
				writerStringbuilder.append(key); 
				writerStringbuilder.append(" "); 
				if (intValuesMapping == null){
					writerStringbuilder.append(data.toLong());
				}else {
					writerStringbuilder.append(intValuesMapping[(int)data.toLong()]);
				}
				
				writerStringbuilder.append("\n");
				logStream.append(writerStringbuilder);
				break;
			case DOUBLE:
			case FLOAT:
				//Log.w(TAG, "Writitng " + getReaderName() + data.toDouble());
				
				if (loggingFloatPrecision < 1) {
					writerStringbuilder.append(key); 
					writerStringbuilder.append(" "); 
					writerStringbuilder.append(data.toDouble()); 
					writerStringbuilder.append("\n");
					logStream.append(writerStringbuilder);
					
					
				} else {
					//TODO fix 
					//logStream.append(String.format("%s %." + loggingFloatPrecision+ "f\n", key, data.toDouble()));
					logStream.append(String.format("%s %.4f\n", key, data.toDouble()));
				}
				
				break;
			case STRING:
			case MIXED_LNUM_STRING:
				writerStringbuilder.append(key); 
				writerStringbuilder.append(" "); 
				if (intValuesMapping == null){
					writerStringbuilder.append(data.toLong()); 
					//logStream.append(key + " " + data.toLong() + " " + data.toString() + "\n");
				}else {
					writerStringbuilder.append(intValuesMapping[(int)data.toLong()]); 
					//logStream.append(key + " " + intValuesMapping[(int)data.toLong()] + " " + data.toString() + "\n");
				}
				
				writerStringbuilder.append(" ");
				writerStringbuilder.append(data.__getStringBuilderRefUnchcked()); 
				writerStringbuilder.append("\n");
				logStream.append(writerStringbuilder);
				
				break;	
			default:
				Log.e(TAG, key + ": Unknown value type to log (" + data.dataType + ")  from  " + getReaderName());
			}
		} catch (Exception e) {
			Log.e(TAG, key + ": Can not write values to log with message " + e.getMessage());
		}
	}	
}
