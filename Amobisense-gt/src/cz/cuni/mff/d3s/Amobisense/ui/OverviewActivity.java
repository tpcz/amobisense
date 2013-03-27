package cz.cuni.mff.d3s.Amobisense.ui;

import android.net.wifi.ScanResult;
import cz.cuni.mff.d3s.Amobisense.context.readers.Accelerometer;
import cz.cuni.mff.d3s.Amobisense.context.readers.BatteryLevel;
import cz.cuni.mff.d3s.Amobisense.context.readers.GSMCells;
import cz.cuni.mff.d3s.Amobisense.context.readers.InternetConnection;
import cz.cuni.mff.d3s.Amobisense.context.readers.Orientation;
import cz.cuni.mff.d3s.Amobisense.context.readers.Proximity;
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
		
	
		
		int graphHeight = 300;
		int graphWith = 0;
		
		config = new MultiPartInfoActivityConfiguration("OverviewTab", null  , null, dataAvailabilityChecker);
		
	    IGraphLongDataCollector nrWiFiSeenCollector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				 
				return WifiContext.getInstance().getMainLongHistoryValues();
				 
			 }
		};
	     
		GraphConfigurationItem graphConfiguration;
		graphConfiguration = config.new GraphConfigurationItem("# Visible WiFi's", nrWiFiSeenCollector);
		graphConfiguration.setAxeLabels("time [s]", "#");
		graphConfiguration.setYAxeLimits(0, 50);
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		graphConfiguration.setOnClickActivity(SeenWifiInfoActivityMP.class);
		
		if (WifiContext.getInstance().isSupported()) {
			config.add(graphConfiguration);
		}
		
		// # battery level in last 60 seconds.
		IGraphDoubleDataCollector dcollector = new IGraphDoubleDataCollector () {
			 public double[] getYValues(int historyLength){
				return Accelerometer.getInstance().getMainDoubleHistoryValues();
			 }
		};
	    
		String title = "1s Cumulated Acceleration"; 
		graphConfiguration = config.new GraphConfigurationItem(title, dcollector);
		graphConfiguration.setAxeLabels("time [s]", "[m^2/s^2]");
		graphConfiguration.setYAxeLimits(0, 35);
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		graphConfiguration.setOnClickActivity(AccelerometerDetailInfoActivityMP.class);
		
		if (Accelerometer.getInstance().isSupported()) {
			config.add(graphConfiguration);
		}

		
		
		IGraphLongDataCollector collector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				return InternetConnection.getInstance().getMainLongHistoryValues();
			 }
		};
	    
		
		graphConfiguration = config.new GraphConfigurationItem("Connection (No/Mobile/Wifi)", collector);
		graphConfiguration.setAxeLabels("time [s]", "NO/M/W");
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		graphConfiguration.setYAxeLimits(-1, 3);
		graphConfiguration.setOnClickActivity(ConnectivityDetailInfoMP.class);
		
		if (InternetConnection.getInstance().isSupported()) {
		//	config.add(graphConfiguration);
		}
		
		IGraphDoubleDataCollector dc = new IGraphDoubleDataCollector () {
			 public double[] getYValues(int historyLength){
				 return Orientation.getInstance().getMainDoubleHistoryValues();
			 }
		};
	    
		
		graphConfiguration = config.new GraphConfigurationItem("Device orientation \u0394/s", dc);
		graphConfiguration.setAxeLabels("time [s]", "\u0394/s");
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		graphConfiguration.setYAxeLimits(0, 100);
		graphConfiguration.setOnClickActivity(OrientationDetailInfoActivityMP.class);
		
		if ( Orientation.getInstance() != null && Orientation.getInstance().isSupported()) {
			config.add(graphConfiguration);
		}
	
		
		IGraphDoubleDataCollector sysPercentCollector = new IGraphDoubleDataCollector () {
			 public double[] getYValues(int historyLength){
				return CPU.getInstanceOrNull().sysHistory.getDHistory( );
			 }
		};
		
		graphConfiguration = config.new GraphConfigurationItem("Load (System)", sysPercentCollector);
		graphConfiguration.setAxeLabels("time [s]", "[%]");
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
		graphConfiguration.setAxeLabels("time [s]", "[%]");
		graphConfiguration.setYAxeLimits(0, 60);
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		graphConfiguration.setOnClickActivity(CPUUsageDetailInfoActivityMP.class);
		
		if (CPU.getInstanceOrNull()!= null) {
			config.add(graphConfiguration);
		}
		
		// # battery level in last 60 seconds.
		collector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				return BatteryLevel.getInstance().getMainLongHistoryValues();
			 }
		};
		
		
	    
		graphConfiguration = config.new GraphConfigurationItem("Battery level", collector);
		graphConfiguration.setAxeLabels("time [min]", "[%]");
		graphConfiguration.setYAxeLimits(0, 110);
		graphConfiguration.setMinDimensions(graphHeight, graphWith);
		graphConfiguration.setOnClickActivity(BatteryLevelDetailInfoActivityMP.class);
		
		if (BatteryLevel.getInstance().isSupported()) {
			//config.add(graphConfiguration);
		}
		
		dcollector = new IGraphDoubleDataCollector () {
			 public double[] getYValues(int historyLength){
				return Proximity.getInstance().getMainDoubleHistoryValues();
			 }
		};
	    
		
		// device front proximity...
		graphConfiguration = config.new GraphConfigurationItem("Device Front Proximity", dcollector);
		graphConfiguration.setAxeLabels("time [s]", "[cm]");
		graphConfiguration.setYAxeLimits(0, (int)Proximity.MAX_VALUE + 1);
		graphConfiguration.setMinDimensions(200, 0);
		graphConfiguration.setOnClickActivity(ProximityMP.class);
		
		if (Proximity.getInstance().isSupported()) {
			config.add(graphConfiguration);
		}
		
		
		IGraphDoubleDataCollector relativeCellFrequency = new IGraphDoubleDataCollector () {
			 public double[] getYValues(int historyLength){
				return GSMCells.getInstance().getDoubleHistoryValues(GSMCells.CURR_CELL_FREQ);
			 }
		};
		
		graphConfiguration = config.new GraphConfigurationItem("Relative freq. of current cell", relativeCellFrequency);
		graphConfiguration.setAxeLabels("time [s]", "[# sec on it/total time]");
		graphConfiguration.setYAxeLimits(0, 2);
		graphConfiguration.setOnClickActivity(GSMCellsMP.class);
		
		if (GSMCells.getInstance().isSupported()) {
			config.add(graphConfiguration);
		}
			 
		fixGraphSizes(50);
		
	} 
}
