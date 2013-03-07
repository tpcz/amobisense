package cz.cuni.mff.d3s.Amobisense.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MultiPartInfoActivityConfiguration {
	
	public String TAG;
	public String title;
	
	public IStringDataCollector summaryTextCollector;
	
	public  IDataAvailableGetter dataAvailabilityChecker;
	
	
	public MultiPartInfoActivityConfiguration (String TAG, String title, 
											 IStringDataCollector summaryTextCollector, 
											 IDataAvailableGetter dataAvailabilityChecker) {
		this.TAG = TAG;
		this.title = title;
		this.summaryTextCollector = summaryTextCollector;
		this.dataAvailabilityChecker = dataAvailabilityChecker;
	}
	
		
	public void add(UIObjectConfigurationItem item) {
		uiObjects.add(item);
	}
	
	public interface IGraphDataCollector {
	}
	
	public interface IGraphIntDataCollector extends IGraphDataCollector {
		int[] getYValues(int historyLength);
	}
	
	public interface IGraphLongDataCollector extends IGraphDataCollector {
		long[] getYValues(int historyLength);
	}
	
	public interface IGraphDoubleDataCollector extends IGraphDataCollector {
		double[] getYValues(int historyLength);
	}	
	
	public interface IListDataCollector<ItemType> {
		Iterable<ItemType> getListItems();
	} 
	
	public interface IStringDataCollector {
		 String getString();
	}
	
	public interface IDataAvailableGetter {
		 Boolean areDataAvailable();
	}
	
	
	public class UIObjectConfigurationItem {
		
	}
	
	/* represents configuration of a single graph */
	public class GraphConfigurationItem extends UIObjectConfigurationItem {
		public String name = "Unnamed Graph";
		public int YAxeMin = 0; 
		public int YAxeMax = 100;
		
		public Class<Activity> onClickClazz = null;
		

		
		public int minGraphHeight = 200;
		
		public int minGraphWidth = 0;
		
		public String XAxeLabel = "X Label"; 
		public String YAxeLabel = "Y Label";
		public IGraphDataCollector collector;
		
		GraphConfigurationItem(String name, IGraphDataCollector collector) {
			this.name = name;
			this.collector = collector;
		}
		
		GraphConfigurationItem(String name, int YAxeMin ,int YAxeMax, String XAxeLabel, String YAxeLabel, IGraphDataCollector collector) {
			this.name = name;			
			this.collector = collector;
			this.YAxeMin = YAxeMin;
			this.YAxeMax = YAxeMax;	
			this.XAxeLabel = XAxeLabel;
			this.YAxeLabel = YAxeLabel;	
		}
		
		void setYAxeLimits(int YAxeMin, int YAxeMax) {
			this.YAxeMin = YAxeMin;
			this.YAxeMax = YAxeMax;			
		}
		
		/**
		 * It is not guarantied, that view will be able to achieve them.
		 * 
		 * @param height if 0, will not be applied and auto value (FILL_CONTENT?) will be used
		 * @param width  if 0, will not be applied and auto value (FILL_CONTENT?) will be used
		 */
		void setMinDimensions(int height, int width) {
			this.minGraphHeight = height;
			this.minGraphWidth = width;			
		}
		
		void setAxeLabels(String XAxeLabel, String YAxeLabel) {
			this.XAxeLabel = XAxeLabel;
			this.YAxeLabel = YAxeLabel;	
		}
		
		void setName(String name) {
			this.name = name;			
		}
		
		void setOnClickActivity(Class clazz) {
			this.onClickClazz = clazz;
		}
	}
	
	public class ArrayAdapterWithOnClick<T> extends  ArrayAdapter<T> {
		
		public ArrayAdapterWithOnClick(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}
		
		public void setOnClickListenerOnItem(ListView listView){
			setOnClickListenerOnItem(listView, this);
		}
		
		public String getTitleText (T item) {
			return item.toString();
		}
		
		public void setOnClickListenerOnItem(ListView listView, ArrayAdapterWithOnClick<T> adapter){
			
		}
		
		
	}
	
	public class ListConfigurationItem<ItemType> extends UIObjectConfigurationItem {
		
		ArrayAdapterWithOnClick<ItemType> adapter;
		IListDataCollector<ItemType> collector;
		
		int listWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
		int listHeight = 200; 
		boolean sortBytitleEnabled = true;
		
		public ListConfigurationItem(IListDataCollector<ItemType> collector, ArrayAdapterWithOnClick<ItemType> adapter) {
			this.collector = collector;
			this.adapter = adapter;
		}	
		
		public void setDimensions(int width, int height){
			this.listWidth = width; 
			this.listHeight = height;
		}
		
		public void setSortByTitleEnabled(boolean enabled){
			this.sortBytitleEnabled = enabled;
		}
	}
	
	/* lists or graphs to show */
	List<UIObjectConfigurationItem> uiObjects = new ArrayList<UIObjectConfigurationItem>();

}

