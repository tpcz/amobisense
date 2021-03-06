package cz.cuni.mff.d3s.Amobisense.ui;

import cz.cuni.mff.d3s.Amobisense.context.readers.InternetConnection;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.GraphConfigurationItem;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IDataAvailableGetter;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphLongDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IStringDataCollector;
import cz.cuni.mff.d3s.Amobisense.utils.NoClass;


public class ConnectivityDetailInfoMP extends MultiPartInfoActivity<NoClass> {

	@Override
	protected void setupConfiguration() {
		
		IStringDataCollector summaryTextCollector = new IStringDataCollector() {
			@Override
			public String getString() {
				
				String info = "NOT SET";
			      switch ((int) InternetConnection.getInstance().getCurrentMainData().toLong()) {
			      	case InternetConnection.CONNECTION_TYPE_NOT_CONNECTED: info = "NO";
			      	break;
			      	case InternetConnection.CONNECTION_TYPE_WIFI: info = "WiFi, " + InternetConnection.getInstance().getUnanonymizedData().toString();
			      	break;
			      	case InternetConnection.CONNECTION_TYPE_MOBILE: info = "Mobile";
			      	break;
			      }
				
				return "Connection: " + info; 
			}
		};
		
		IDataAvailableGetter dataAvailabilityChecker = new IDataAvailableGetter() {	
			@Override
			public Boolean areDataAvailable() {
				return InternetConnection.getInstance() != null;
			}
		};
	
		
		
		String title = "Net Connectivity"; 
		String TAG = "InternetConnectionScreen";
		
		config = new MultiPartInfoActivityConfiguration(TAG, title  , summaryTextCollector, dataAvailabilityChecker);

		// # battery level in last 60 seconds.
		IGraphLongDataCollector collector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				return InternetConnection.getInstance().getMainLongHistoryValues();
			 }
		};
	    
		GraphConfigurationItem graphConfiguration;
		graphConfiguration = config.new GraphConfigurationItem(title, collector);
		graphConfiguration.setAxeLabels("time [s]", "(0: NO, 1: mobile, 2: Wifi)");
		graphConfiguration.setYAxeLimits(-1, 3);
		
		config.add(graphConfiguration);
		
		fixGraphSizes(300);
	} 
}