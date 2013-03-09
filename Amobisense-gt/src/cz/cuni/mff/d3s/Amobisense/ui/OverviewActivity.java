package cz.cuni.mff.d3s.Amobisense.ui;

import android.net.wifi.ScanResult;
import cz.cuni.mff.d3s.Amobisense.context.readers.Accelerometer;
import cz.cuni.mff.d3s.Amobisense.context.readers.BatteryLevel;
import cz.cuni.mff.d3s.Amobisense.context.readers.InternetConnection;
import cz.cuni.mff.d3s.Amobisense.context.readers.WifiContext;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.GraphConfigurationItem;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IDataAvailableGetter;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphDoubleDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphLongDataCollector;
import edu.umich.PowerTutor.dataReaders.CPU;
import edu.umich.PowerTutor.util.BatteryStats;


public class OverviewActivity extends MultiPartInfoActivity<ScanResult> {

	
	@Override
	protected void setupConfiguration() {

		showTitleAndtext = false;
		
		IDataAvailableGetter dataAvailabilityChecker = new IDataAvailableGetter() {
			@Override
			public Boolean areDataAvailable() {
				return 
						CPU.getInstanceOrNull()   != null
						&&
						WifiContext.getInstance() != null
						&&
						BatteryStats.getInstance()!= null
						&&
						Accelerometer.getInstance()!= null
						&&
						Accelerometer.getInstance().isSupported()
						&&
						InternetConnection.getInstance()!= null
						&&
						true
						;
			}
		};
		
		int graphHeight = 100;
		int graphWith = 0; // auto value (fill)
		
		config = new MultiPartInfoActivityConfiguration("OverviewTab", null  , null, dataAvailabilityChecker);
		
		  // # seen wifi graph
	    IGraphLongDataCollector nrWiFiSeenCollector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				return ((WifiContext)WifiContext.getInstance()).getMainLongHistoryValues();
			 }
		};
	     
		GraphConfigurationItem graphConfiguration;
		graphConfiguration = config.new GraphConfigurationItem("Nr Seen WiFi's", nrWiFiSeenCollector);
		graphConfiguration.setAxeLabels("time [s]", "Num WiFi's");
		graphConfiguration.setYAxeLimits(0, 50);
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		graphConfiguration.setOnClickActivity(SeenWifiInfoActivityMP.class);
		
		config.add(graphConfiguration);
		
		// # battery level in last 60 seconds.
		IGraphDoubleDataCollector dcollector = new IGraphDoubleDataCollector () {
			 public double[] getYValues(int historyLength){
				return Accelerometer.getInstance().getMainDoubleHistoryValues();
			 }
		};
	    
		String title = "Accelerometer Activity"; 
		graphConfiguration = config.new GraphConfigurationItem(title, dcollector);
		graphConfiguration.setAxeLabels("time [s]", "Cum. Eue. Act [m^2/s]");
		graphConfiguration.setYAxeLimits(0, 35);
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		graphConfiguration.setOnClickActivity(AccelerometerDetailInfoActivityMP.class);
		
		config.add(graphConfiguration);

		
		// # battery level in last 60 seconds.
		IGraphLongDataCollector collector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				return InternetConnection.getInstance().getMainLongHistoryValues();
			 }
		};
	    
		
		graphConfiguration = config.new GraphConfigurationItem("Net connection", collector);
		graphConfiguration.setAxeLabels("time [s]", "0: NO, 1: M, 2: WF");
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		graphConfiguration.setYAxeLimits(0, 3);
		graphConfiguration.setOnClickActivity(ConnectivityDetailInfoMP.class);
		
		config.add(graphConfiguration);
	
		
		IGraphDoubleDataCollector sysPercentCollector = new IGraphDoubleDataCollector () {
			 public double[] getYValues(int historyLength){
				return CPU.getInstanceOrNull().sysHistory.getDHistory( );
			 }
		};
		
		graphConfiguration = config.new GraphConfigurationItem("Load (System)", sysPercentCollector);
		graphConfiguration.setAxeLabels("time [s]", "Load [%]");
		graphConfiguration.setYAxeLimits(0, 40);
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		//config.add(graphConfiguration);
		
		// User
		IGraphDoubleDataCollector sysUsrCollector = new IGraphDoubleDataCollector () {
				public double[] getYValues(int historyLength){
						return CPU.getInstanceOrNull().usrHistory.getDHistory( );
				}
		};
		
		graphConfiguration = config.new GraphConfigurationItem("Load (User)", sysUsrCollector);
		graphConfiguration.setAxeLabels("time [s]", "Load [%]");
		graphConfiguration.setYAxeLimits(0, 60);
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		config.add(graphConfiguration);
		graphConfiguration.setOnClickActivity(CPUUsageDetailInfoActivityMP.class);
		
		
		// # battery level in last 60 seconds.
		collector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				return BatteryLevel.getInstance().getMainLongHistoryValues();
			 }
		};
	    
		graphConfiguration = config.new GraphConfigurationItem("Battery level", collector);
		graphConfiguration.setAxeLabels("time [min]", "Level [%]");
		graphConfiguration.setYAxeLimits(0, 110);
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		graphConfiguration.setOnClickActivity(BatteryLevelDetailInfoActivityMP.class);
		
		config.add(graphConfiguration);
			
	} 
}
