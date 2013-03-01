package cz.cuni.mff.d3s.Amobisense.ui;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import cz.cuni.mff.d3s.Amobisense.R;
import cz.cuni.mff.d3s.Amobisense.context.readers.WifiContext;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.ArrayAdapterWithOnClick;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.GraphConfigurationItem;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IDataAvailableGetter;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphLongDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IListDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IStringDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.ListConfigurationItem;


public class SeenWifiInfoActivityMP extends MultiPartInfoActivity<ScanResult> {

	@Override
	protected void setupConfiguration() {
		
		IStringDataCollector summaryTextCollector = new IStringDataCollector() {
			@Override
			public String getString() {
				return "Right now seeing: " + WifiContext.getInstance().getCurrentMainData().toLong() + " WiFi's";
			}
		};
		
		IDataAvailableGetter dataAvailabilityChecker = new IDataAvailableGetter() {	
			@Override
			public Boolean areDataAvailable() {
				return WifiContext.getInstance() != null;
			}
		};
	
		
		
		
		String title = "Visible WiFi's"; 
		String TAG = "SeenWiFiInfo";
		
		config = new MultiPartInfoActivityConfiguration(TAG, title  , summaryTextCollector, dataAvailabilityChecker);
		
		
		// list of currently visible wifi
		IListDataCollector<ScanResult> collector = new IListDataCollector<ScanResult>() {
			@Override
			public Iterable<ScanResult> getListItems() {
				return WifiContext.getInstance().getScanResults();
			}
	    };
	    
	    
	    final ArrayAdapterWithOnClick<ScanResult> adapter = config.new ArrayAdapterWithOnClick<ScanResult>(this, 0) {
	    	  public View getView(int position, View convertView, ViewGroup parent) {
		       
		    	View itemView = getLayoutInflater().inflate(R.layout.signal_item_layout, new ListView(SeenWifiInfoActivityMP.this), false);
		        TextView title = (TextView)itemView.findViewById(R.id.title);
		       
		        LinearLayout widgetGroup = (LinearLayout)itemView.findViewById(R.id.widget_frame);
		        ScanResult item = getItem(position);
		        
		        setupView(title, getTitleText(item), widgetGroup);
		        
		        return itemView;
		      }
		      
	    	  @Override
	    	  public String getTitleText(ScanResult item){
	    		  return item.SSID + ", " + item.BSSID;
	    	  }
	    	  
		      private void setupView(TextView title, String titleText, LinearLayout widgetGroup) {
		      		title.setText(titleText);
		      }
		      
		      @Override
		      public void setOnClickListenerOnItem(ListView listView, final ArrayAdapterWithOnClick<ScanResult> adapter){
		    	  listView.setOnItemClickListener(new OnItemClickListener() {
		    	  	  @Override
		    	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		    	            ScanResult item = (ScanResult) adapter.getItem(position); 
		    	            	Intent i = new Intent(SeenWifiInfoActivityMP.this, WiFiDetailInfoActivityMP.class);
		    	                i.putExtra("ap-bssid", item.BSSID);
		    	                i.putExtra("ap-ssid",  item.SSID);
		    	                SeenWifiInfoActivityMP.this.startActivity(i);
		    	         }
		    	      });
			  }
	    };
	    
	    ListConfigurationItem<ScanResult> visibleWiFiList = config.new ListConfigurationItem<ScanResult>(collector, adapter);
	    visibleWiFiList.setDimensions(ViewGroup.LayoutParams.WRAP_CONTENT, 200);
	    
	    config.add(visibleWiFiList);
	    
	    // # seen wifi graph
	    IGraphLongDataCollector nrWiFiSeenCollector = new IGraphLongDataCollector () {
			 public long[] getYValues(int historyLength){
				return (WifiContext.getInstance()).getMainLongHistoryValues();
			 }
		};
	    
		GraphConfigurationItem graphConfiguration;
		graphConfiguration = config.new GraphConfigurationItem("WiFi Availability", nrWiFiSeenCollector);
		graphConfiguration.setAxeLabels("time [s]", "Num WiFi's");
		graphConfiguration.setYAxeLimits(0, 50);
		
		config.add(graphConfiguration);
	} 
}