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
	
		
		
		String title = "Cummulated Acceleration/s"; 
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
		graphConfiguration.setAxeLabels("time [s]", "Cum. Eue. Acc [m^3/s]");
		graphConfiguration.setYAxeLimits(0, 40);
		graphConfiguration.setMinDimensions(120, 0);
		
		config.add(graphConfiguration);
		
		title = "Acceleration - dim"; 
		// x
		collector = new IGraphDoubleDataCollector () {
					 public double[] getYValues(int historyLength){
						return Accelerometer.getInstance().getDoubleHistoryValues(Accelerometer.GRAVITY_X);
					 }
		};
		
		graphConfiguration = config.new GraphConfigurationItem(title + " X", collector);
		graphConfiguration.setAxeLabels("time [s]", "Accel [m^2/s]");
		graphConfiguration.setYAxeLimits(-15, 15);
		graphConfiguration.setMinDimensions(100, 0);
		
		config.add(graphConfiguration);
		// y
		collector = new IGraphDoubleDataCollector () {
					 public double[] getYValues(int historyLength){
						return Accelerometer.getInstance().getDoubleHistoryValues(Accelerometer.GRAVITY_Y);
					 }
		};
		
		graphConfiguration = config.new GraphConfigurationItem(title + " Y", collector);
		graphConfiguration.setAxeLabels("time [s]", "Accel [m^2/s]");
		graphConfiguration.setYAxeLimits(-15, 15);
		graphConfiguration.setMinDimensions(100, 0);
		config.add(graphConfiguration);
		// z
		collector = new IGraphDoubleDataCollector () {
					 public double[] getYValues(int historyLength){
						return Accelerometer.getInstance().getDoubleHistoryValues(Accelerometer.GRAVITY_Z);
					 }
		};
		
		graphConfiguration = config.new GraphConfigurationItem(title + " Z", collector);
		graphConfiguration.setAxeLabels("time [s]", "Accel [m^2/s]");
		graphConfiguration.setYAxeLimits(-15, 15);
		graphConfiguration.setMinDimensions(120, 0);
		
		
		config.add(graphConfiguration);
	} 
}