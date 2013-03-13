package cz.cuni.mff.d3s.Amobisense.context.readers;

import java.io.OutputStreamWriter;
import java.util.HashMap;

import cz.cuni.mff.d3s.Amobisense.context.ContextData;
import cz.cuni.mff.d3s.Amobisense.context.HistoryHolder;

public interface IDataReader {
	
	/** writes all the current data to a log */
	void writeLog(OutputStreamWriter contextLogStream, long iter, boolean writeOnlyOnChange);
	
	/** get name of the reader */
	String getReaderName();
	
	
	/** gets current value from the reader */
	HashMap<String, ContextData> getCurrentData();
	
	/** gets history values */
	HashMap<String, HistoryHolder>  getHistory();
	
	/** free all the resources */
	public void clearResources();
	
	public boolean isSupported();
}
