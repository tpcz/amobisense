package cz.cuni.mff.d3s.Amobisense.ui;

import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.GraphConfigurationItem;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IDataAvailableGetter;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphDoubleDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IStringDataCollector;
import cz.cuni.mff.d3s.Amobisense.utils.NoClass;
import edu.umich.PowerTutor.dataReaders.CPU;


public class CPUUsageDetailInfoActivityMP extends MultiPartInfoActivity<NoClass> {
	
	@Override
	protected void setupConfiguration() {
		
		IStringDataCollector summaryTextCollector = new IStringDataCollector() {
			@Override
			public String getString() {
				return String.format("Currently using %.1f SYS and %.1f USR ",
						CPU.getInstanceOrNull().getSysPerc(), CPU.getInstanceOrNull().getUsrPerc());
			}
		};
		
		IDataAvailableGetter dataAvailabilityChecker = new IDataAvailableGetter() {
			@Override
			public Boolean areDataAvailable() {
				return CPU.getInstanceOrNull() != null;
			}
		};
	
		GraphConfigurationItem graph;
		
		// TODO
		config = new MultiPartInfoActivityConfiguration("CPUDetail", "CPU Load Details"  , summaryTextCollector, dataAvailabilityChecker);
		
		// Sys
		IGraphDoubleDataCollector sysPercentCollector = new IGraphDoubleDataCollector () {
			 public double[] getYValues(int historyLength){
				return CPU.getInstanceOrNull().sysHistory.getDHistory( );
			 }
		};
		
		graph = config.new GraphConfigurationItem("System Load", sysPercentCollector);
		graph.setAxeLabels("time [s]", "Load [%]");
		graph.setYAxeLimits(0, 100);
		config.add(graph);
		
		// User
		IGraphDoubleDataCollector sysUsrCollector = new IGraphDoubleDataCollector () {
				public double[] getYValues(int historyLength){
						return CPU.getInstanceOrNull().usrHistory.getDHistory( );
				}
		};
		
		graph = config.new GraphConfigurationItem("User Space Load", sysUsrCollector);
		graph.setAxeLabels("time [s]", "Load [%]");
		graph.setYAxeLimits(0, 100);
		config.add(graph);
	} 
}
