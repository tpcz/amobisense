package cz.cuni.mff.d3s.Amobisense.ui;

import cz.cuni.mff.d3s.Amobisense.context.readers.BatteryLevel;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.GraphConfigurationItem;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IDataAvailableGetter;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphLongDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IStringDataCollector;


public class BatteryLevelDetailInfoActivityMP extends MultiPartInfoActivity<NoClass> {

	@Override
	protected void setupConfiguration() {
		
		IStringDataCollector summaryTextCollector = new IStringDataCollector() {
			@Override
			public String getString() {
				return "Battery level now is: " + BatteryLevel.getInstance().getCurrentMainData().toLong() + "%";
			}
		};
		
		IDataAvailableGetter dataAvailabilityChecker = new IDataAvailableGetter() {	
			@Override
			public Boolean areDataAvailable() {
				return BatteryLevel.getInstance() != null;
			}
		};
	
		
		String title = "Battery Level"; 
		String TAG = "BatteryLevelDetail";
		
		config = new MultiPartInfoActivityConfiguration(TAG, title  , summaryTextCollector, dataAvailabilityChecker);

		// # battery level in last 60 seconds.
		IGraphLongDataCollector collector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				return BatteryLevel.getInstance().getMainLongHistoryValues();
			 }
		};
	    
		GraphConfigurationItem graphConfiguration;
		graphConfiguration = config.new GraphConfigurationItem("Battery level", collector);
		graphConfiguration.setAxeLabels("time [min]", "Level [%]");
		graphConfiguration.setYAxeLimits(0, 110);
		
		config.add(graphConfiguration);
	} 
}