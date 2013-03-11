/*
Copyright (C) 2011 The University of Michigan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please send inquiries to powertutor@umich.edu
*/

package cz.cuni.mff.d3s.Amobisense.ui;

import android.app.TabActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TabHost;
import cz.cuni.mff.d3s.Amobisense.R;

public class ContextTabs extends TabActivity {
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.power_tabs);

    TabHost tabHost = getTabHost();
    TabHost.TabSpec spec;

    Intent intent;

    intent = new Intent(this, MiscView.class);
    intent.putExtras(getIntent());
    spec = tabHost.newTabSpec("Stat").setIndicator("Detail View").setContent(intent);
    tabHost.addTab(spec);
    
    
    intent = new Intent(this, OverviewActivity.class);
    intent.putExtras(getIntent());
    spec = tabHost.newTabSpec("Charts").setIndicator("History Graphs").setContent(intent);
    tabHost.addTab(spec);

    Uri uri = getIntent().getData();
    tabHost.setCurrentTab(0);
    
    if (uri.getQuery().contains("details")) {
    	tabHost.setCurrentTab(0);
    } else if (uri.getQuery().contains("graphs")) {
    	tabHost.setCurrentTab(1);
    }
  }
}
