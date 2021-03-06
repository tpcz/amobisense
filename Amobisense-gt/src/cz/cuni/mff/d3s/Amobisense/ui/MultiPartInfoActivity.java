package cz.cuni.mff.d3s.Amobisense.ui;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.achartengine.GraphicalView;
import org.achartengine.chart.CubicLineChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cz.cuni.mff.d3s.Amobisense.R;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.ArrayAdapterWithOnClick;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.GraphConfigurationItem;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphDoubleDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphIntDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IGraphLongDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.IListDataCollector;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.ListConfigurationItem;
import cz.cuni.mff.d3s.Amobisense.ui.MultiPartInfoActivityConfiguration.UIObjectConfigurationItem;
import edu.umich.PowerTutor.service.DataCollector;
import edu.umich.PowerTutor.ui.PowerPie;

public abstract class MultiPartInfoActivity<ListItemType> extends Activity {

	private SharedPreferences prefs;

	private int lastLayoutId;

	protected MultiPartInfoActivityConfiguration config;

	protected boolean summaryAsHTML;

	// private ArrayList<GraphHandle> graphHandles = new
	// ArrayList<GraphHandle>();

	// should be overidden in subclasses
	private static final String TAG = "MultiGraphInfoActivity";

	private Runnable textCollector;
	private Handler handler;
	private LinearLayout chartLayout;
	boolean showTitleAndtext = true;
	RelativeLayout rlayout;

	abstract protected void setupConfiguration();

	@Override
	protected void onResume() {
		super.onResume();
		handler = new Handler();

		// getApplicationContext().bindService(serviceIntent, conn, 0);
		setupConfiguration();
		setupActivityViewFromXML();
		setupConfiguration();
		findViewById(R.id.detail_layout).invalidate();
		setupActivityView();
		graphCounter = 1;
		
		
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (showTitleAndtext) {
			setContentView(R.layout.multipart_info_titled);
		} else {
			setContentView(R.layout.multipart_info_notitle);
		}
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		rlayout = (RelativeLayout) findViewById(R.id.detail_layout);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// getApplicationContext().unbindService(conn);
		if (textCollector != null) {
			handler.removeCallbacks(textCollector);
			textCollector = null;
			handler = null;
		}
	}

	public boolean dataAvailable() {
		return config.dataAvailabilityChecker.areDataAvailable();
		// return (null != WifiContext.getInstance());
	}

	private void setupView() {
		if (showTitleAndtext) {
			TextView t = (TextView) findViewById(R.id.summary);
			if (dataAvailable()) {
				if (!summaryAsHTML) {
					t.setText(config.summaryTextCollector.getString());
				} else {
					t.setText(Html.fromHtml(config.summaryTextCollector.getString()));
				}
			} else {
				t.setText("Data not available");
			}
		}
	}

	private void setUpGraphLayout(XYMultipleSeriesRenderer renderer) {
		int numVals = Integer.parseInt(prefs.getString("viewNumValues_s", "60"));
		renderer.clearXTextLabels();
		renderer.setXAxisMin(0);
		renderer.setXAxisMax(numVals - 1);
		renderer.addXTextLabel(numVals - 1, "" + numVals);
		renderer.setXLabels(0);
		for (int j = 0; j < 10; j++) {
			renderer.addXTextLabel(numVals * j / 10, "" + (1 + numVals * j / 10));
		}
	}

	private static int graphCounter = 1;

	private void addList(final ListConfigurationItem<ListItemType> config) {

		ListView listView = new ListView(this);

		Iterable<ListItemType> items = config.collector.getListItems();
		if (items == null) {
			Log.w(TAG, "Empty list item should be constructed from empty collection, not a null!");
			return;
		}

		if (config.sortBytitleEnabled) {
			SortedSet<ListItemType> ts = new TreeSet<ListItemType>(new Comparator<ListItemType>() {
				@Override
				public int compare(ListItemType lhs, ListItemType rhs) {
					return config.adapter.getTitleText(lhs).compareToIgnoreCase(config.adapter.getTitleText(rhs));
				}
			});

			for (ListItemType inf : items) {
				ts.add(inf);
			}

			items = ts;
		}

		for (ListItemType inf : items) {
			config.adapter.add(inf);
		}

		listView.setAdapter(config.adapter);
		listView.setFastScrollEnabled(true);
		//listView.setScrollbarFadingEnabled(false);

		// final ArrayAdapter adapter = listConfiguration.adapter;

		config.adapter.setOnClickListenerOnItem(listView);

		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(config.listWidth, config.listHeight);
		p.setMargins(0, 1, 0, 0);
		p.height = config.listHeight;
		p.width = config.listWidth;
		p.addRule(RelativeLayout.BELOW, lastLayoutId);
		
		
		
		rlayout = (RelativeLayout) findViewById(R.id.detail_layout);
		rlayout.addView(listView, p);
		
		listView.setId(++lastLayoutId);
		
		ListHandle listHandle = new ListHandle(listView, config.collector, config.adapter);

		if (handler != null) {
			handler.post(listHandle);
		}
		
	}

	private void addGraph(final GraphConfigurationItem  graphConfiguration) {
		XYSeries series = new XYSeries(graphConfiguration.name);
		XYMultipleSeriesDataset mseries = new XYMultipleSeriesDataset();
		mseries.addSeries(series);

		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		XYSeriesRenderer srenderer = new XYSeriesRenderer();
		renderer.setYAxisMin(graphConfiguration.YAxeMin);
		Integer.parseInt(prefs.getString("viewNumValues_s", "60"));
		renderer.setXAxisMax(Integer.parseInt(prefs.getString("viewNumValues_s", "60")));
		renderer.setYAxisMax(graphConfiguration.YAxeMax);
		renderer.setYTitle(graphConfiguration.name);
		renderer.setXTitle(graphConfiguration.XAxeLabel);
		renderer.setYTitle(graphConfiguration.YAxeLabel);
		renderer.setAxisTitleTextSize(18);
		renderer.setChartTitleTextSize(25);
		renderer.setShowLegend(false);
		renderer.setChartTitle(graphConfiguration.name);

		int clr = PowerPie.COLORS[(graphCounter++) % PowerPie.COLORS.length];
		srenderer.setColor(clr);
		srenderer.setFillBelowLine(true);
		srenderer.setFillBelowLineColor(((clr >> 1) & 0x7F7F7F) | (clr & 0xFF000000));

		renderer.addSeriesRenderer(srenderer);
		int[] margins = renderer.getMargins();
		margins[1] += 1;
		margins[3] = 0;
		//margins[1] = 0;
		//margins[2] = 0;
		
		renderer.setMargins(margins);

		View chartView = new GraphicalView(this, new CubicLineChart(mseries, renderer, 0.5f));

		((GraphicalView) chartView).repaint();

		if (graphConfiguration.minGraphHeight > 0) {
			chartView.setMinimumHeight(graphConfiguration.minGraphHeight);
		}
		if (graphConfiguration.minGraphWidth > 0) {
			chartView.setMinimumWidth(graphConfiguration.minGraphWidth);
		}

		setUpGraphLayout(renderer);
		
		if (graphConfiguration.onClickClazz != null) {
			chartView.setOnTouchListener(new View.OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					  if (event.getAction() == MotionEvent.ACTION_DOWN){
						  Intent i = new Intent(MultiPartInfoActivity.this, graphConfiguration.onClickClazz);
						  MultiPartInfoActivity.this.startActivity(i);
						  
					  }
					  return false;
				}
			});
		}
		//LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(new MarginLayoutParams(, 0));
		
		
		chartLayout.addView(chartView);

		GraphHandle newGraphHandle = new GraphHandle(series, chartView, graphConfiguration.collector);
		if (handler != null) {
			handler.post(newGraphHandle);
		}
	}

	public void setupActivityViewFromXML() {
		if (showTitleAndtext) {
			setContentView(R.layout.multipart_info_titled);
		} else {
			setContentView(R.layout.multipart_info_notitle);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setupActivityView() {
		if (showTitleAndtext) {
			textCollector = new Runnable() {
				public void run() {
					setupView();
					if (handler != null) {
						handler.postDelayed(this, DataCollector.ITERATION_INTERVAL);
					}
				}
			};

			// set title
			TextView t = (TextView) findViewById(R.id.title);
			t.setText(config.title);

			lastLayoutId = R.id.summary;
		} else {
			lastLayoutId = R.id.detail_layout;
		}

		// rest (graphs and lists) will be added dynamically
		chartLayout = new LinearLayout(this);
		chartLayout.setOrientation(LinearLayout.VERTICAL);

		for (UIObjectConfigurationItem item : config.uiObjects) {
			if (item instanceof GraphConfigurationItem) {
				addGraph((GraphConfigurationItem) item);
			} else if (item instanceof ListConfigurationItem) {
				addList((ListConfigurationItem) item);
			} else {
				Log.w(TAG, "Tryin to add non configuration object to activity");
			}
		}
		
		rlayout.setPadding(0, 0, 0, 0);

		
		rotateChartViewContainer();
		
		// text part

		if (handler != null) {
			handler.post(textCollector);
		}
	}
	
	private void rotateChartViewContainer() {
		chartLayout.setMinimumHeight(200);
		RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		p.addRule(RelativeLayout.BELOW, lastLayoutId);
		p.setMargins(0, 0, 0, 0);
		p.width = ViewGroup.LayoutParams.FILL_PARENT;

		chartLayout.setId(++lastLayoutId);
		ScrollView scrollView = new ScrollView(this);
		
		scrollView.addView(chartLayout);
		scrollView.setId(++lastLayoutId);
		
		
		rlayout = (RelativeLayout) findViewById(R.id.detail_layout);
		rlayout.addView(scrollView, p);
	}
	
	protected void fixGraphSizes(int extraSpace) {
		// correct graph widths;
		DisplayMetrics metric = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metric);
		
		int numGraphs = 0;

		for (UIObjectConfigurationItem c : config.uiObjects) {
			if (c instanceof GraphConfigurationItem) {
				numGraphs ++;
			}
		}

		int[] margins =  new XYMultipleSeriesRenderer().getMargins();
		int GraphMargins = margins[0] + margins[2]; 
		
		int gh = (metric.heightPixels - (numGraphs * GraphMargins) - extraSpace) / numGraphs;
		
		
		for (UIObjectConfigurationItem c : config.uiObjects) {
			if (c instanceof GraphConfigurationItem) {
				((GraphConfigurationItem) c).setMinDimensions(gh, 0);
			}
		}
	}

	public class ListHandle implements Runnable {

		ListView listView;
		IListDataCollector<ListItemType> collector;
		ArrayAdapterWithOnClick<ListItemType> adapter;
		private long lastTime;

		public ListHandle(ListView listView, IListDataCollector<ListItemType> collector,
				ArrayAdapterWithOnClick<ListItemType> adapter) {
			this.listView = listView;
			this.collector = collector;
			this.adapter = adapter;
			this.lastTime = SystemClock.elapsedRealtime();
		}

		public void run() {
			adapter.clear();

			// final Iterable<ScanResult> allItems =
			// ((WifiData)WifiContext.getInstance().getCurrentData()).getScanResults();

			final Iterable<ListItemType> allItems = collector.getListItems();

			for (ListItemType inf : allItems) {
				adapter.add(inf);
			}

			long curTime = SystemClock.elapsedRealtime();
			long tryTime = lastTime + DataCollector.ITERATION_INTERVAL
					* (long) Math.max(1, 1 + (curTime - lastTime) / DataCollector.ITERATION_INTERVAL);

			if (handler != null) {
				handler.postDelayed(this, tryTime - curTime);
			}

			listView.invalidate();
		}

	}

	public class GraphHandle implements Runnable {
		private XYSeries series;
		private View chartView;
		private long lastTime;
		private IGraphDataCollector collector;

		public GraphHandle(XYSeries series, View chartView, IGraphDataCollector collector) {
			this.series = series;
			this.chartView = chartView;
			lastTime = SystemClock.elapsedRealtime();
			this.collector = collector;
			reset();
		}

		/** Restart points collecting from zero. */
		public void reset() {
			series.clear();
		}

		public void run() {

			if (collector != null) {
				series.clear();

				if (collector instanceof IGraphIntDataCollector) {

					int[] values = ((IGraphIntDataCollector) this.collector).getYValues(Integer.parseInt(prefs
							.getString("viewNumValues_s", "60")));
					if (values == null) {
						Log.e("MultipartInfo:", "Values Are Null!");
						return;
					}
					for (int i = 0; i < values.length; i++) {
						series.add(i, values[i]);
					}
					series.add(values.length, 0);
				} else if (collector instanceof IGraphLongDataCollector) {
					long[] values = ((IGraphLongDataCollector) this.collector).getYValues(Integer.parseInt(prefs
							.getString("viewNumValues_s", "60")));

					if (values == null) {
						Log.e("MultipartInfo:", "Values Are Null!");
						return;
					}
					for (int i = 0; i < values.length; i++) {
						series.add(i, values[i]);
					}
					series.add(values.length, 0);
				} else if (collector instanceof IGraphDoubleDataCollector) {
					double[] values = ((IGraphDoubleDataCollector) this.collector).getYValues(Integer.parseInt(prefs.getString("viewNumValues_s", "60")));

					if (values == null) {
						Log.e("MultipartInfo:", "Values Are Null!");
						return;
					}
					
					for (int i = 0; i < values.length; i++) {
						series.add(i, values[i]); 
					}
					series.add(values.length, 0); 
				}
			}

			long curTime = SystemClock.elapsedRealtime();
			long tryTime = lastTime + DataCollector.ITERATION_INTERVAL
					* (long) Math.max(1, 1 + (curTime - lastTime) / DataCollector.ITERATION_INTERVAL);
			if (handler != null) {
				handler.postDelayed(this, tryTime - curTime);
			} else {
				Log.w(TAG, "Handler is null (OK, when activity closed recently)...");
			}

			chartView.invalidate();
		}
		
		

	};
}
