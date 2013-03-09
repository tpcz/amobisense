package cz.cuni.mff.d3s.Amobisense.ui;

import android.content.Intent;
import android.net.wifi.ScanResult;
import cz.cuni.mff.d3s.Amobisense.context.readers.WifiContext;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.GraphConfigurationItem;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IDataAvailableGetter;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphLongDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IStringDataCollector;


public class WiFiDetailInfoActivityMP extends MultiPartInfoActivity<ScanResult> {

	private String BSSID=null;
	private String SSID = null;
	//private String netIdSignal = null;
	//private String netIdVisibility = null;
	
	@Override
	protected void setupConfiguration() {
		
		IStringDataCollector summaryTextCollector = new IStringDataCollector() {
			@Override
			public String getString() {
				return String.format("(%s)... and seeing %d other Wi-Fi's", BSSID, Math.max(WifiContext.getInstance().getCurrentMainData().toLong() -1, 0) );
			}
		};
		
		IDataAvailableGetter dataAvailabilityChecker = new IDataAvailableGetter() {
			
			@Override
			public Boolean areDataAvailable() {
				return WifiContext.getInstance() != null;
			}
		};
	
		Intent i = getIntent();
		BSSID = i.getStringExtra("ap-bssid");
		SSID = i.getStringExtra("ap-ssid");
		
		//netIdSignal = i.getStringExtra("net-id-signal");
		//netIdVisibility = i.getStringExtra("net-id-visibility");
		
		
		String id = WifiContext.getInstance().getWifiLogId(BSSID, SSID);
		final String vnc = "WIFI-" + id + "-VISIBILITY";
		final String snc = "WIFI-" + id + "-SIGNAL";
		
		GraphConfigurationItem graph;
		
		config = new MultiPartInfoActivityConfiguration("WifiDerail", SSID + " details"  , summaryTextCollector, dataAvailabilityChecker);
		
		// add first graph  - wifi connectivity;
		IGraphLongDataCollector wasWifiAvailableCollector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				return (WifiContext.getInstance()).getLongHistoryValues(vnc);
			 }
		};
		
		graph = config.new GraphConfigurationItem("WiFi Availability", wasWifiAvailableCollector);
		graph.setAxeLabels("time [s]", "A (1)/NA(0)");
		graph.setYAxeLimits(0, 2);
		//graph.setMinDimensions(height, width);
		config.add(graph);
		
		// add second graph  - wifi signal;
		IGraphLongDataCollector signalStrengthCollector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				 return (WifiContext.getInstance()).getLongHistoryValues(snc);
			 }
		};
		
		graph = config.new GraphConfigurationItem("Signal Strength [dBm]", signalStrengthCollector);
		graph.setAxeLabels("time [s]", "[dBm]");
		graph.setYAxeLimits(-100, 0);
		config.add(graph);
	} 
}
