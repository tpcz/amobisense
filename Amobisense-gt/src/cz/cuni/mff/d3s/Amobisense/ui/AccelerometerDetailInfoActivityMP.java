package cz.cuni.mff.d3s.Amobisense.ui;

import cz.cuni.mff.d3s.Amobisense.context.readers.Accelerometer;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.GraphConfigurationItem;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IDataAvailableGetter;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphDoubleDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IStringDataCollector;


public class AccelerometerDetailInfoActivityMP extends MultiPartInfoActivity<NoClass> {

	@Override
	protected void setupConfiguration() {
		
		IStringDataCollector summaryTextCollector = new IStringDataCollector() {
			@Override
			public String getString() {
				return String.format("Last second cumulated eucl. activity is: %.2f " ,
						Accelerometer.getInstance().getCurrentMainData().toDouble());
			}
		};
		
		IDataAvailableGetter dataAvailabilityChecker = new IDataAvailableGetter() {	
			@Override
			public Boolean areDataAvailable() {
				return Accelerometer.getInstance() != null &&
				Accelerometer.getInstance().isSupported();
			}
		};
	
		
		
		String title = "Accelerometer Activity"; 
		String TAG = "AccelerometerActivityScreen";
		
		config = new MultiPartInfoActivityConfiguration(TAG, title  , summaryTextCollector, dataAvailabilityChecker);

		// # battery level in last 60 seconds.
		IGraphDoubleDataCollector collector = new IGraphDoubleDataCollector () {
			 public double[] getYValues(int historyLength){
				return Accelerometer.getInstance().getMainDoubleHistoryValues();
			 }
		};
	    
		GraphConfigurationItem graphConfiguration;
		graphConfiguration = config.new GraphConfigurationItem(title, collector);
		graphConfiguration.setAxeLabels("time [s]", "Cum. Eue. Act [m^2/s]");
		graphConfiguration.setYAxeLimits(0, 35);
		
		config.add(graphConfiguration);
	} 
}