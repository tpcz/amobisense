/*
Copyright (C) 2011 The University of Michigan, modified
2013 by Tomas Pop, Charles University in Prague.

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

 */

package cz.cuni.mff.d3s.Amobisense.context.readers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import edu.umich.PowerTutor.phone.PhoneConstants;

/**
 * Abstract class for context state holders based on broadcast receivers.
 * 
 * @author pop
 * 
 */
public abstract class AbstractBroadcastEventReader extends AbstractEventReader implements IDataReader {
		
	//protected Intent intent = null;
	protected BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent i) {
			// TODO Auto-generated method stub
			AbstractBroadcastEventReader.this.onReceive(c, i);
		}
	};
	protected boolean stopHistory = false;
	
	/** should be changed in subclasses! */
	public static final String TAG = "BroadcastReader";
	
	/** Is called once from constructor */
	public void init() {
		
		
	}
	
	protected abstract void onReceive(Context c, Intent i);
	
	public Intent registerReceiver(IntentFilter filter) {
		return this.c.registerReceiver(this.receiver, filter);
	}
	
	
	public void unregisterReceiver(){
		this.c.unregisterReceiver(this.receiver);
	}
	
	public String getReaderType() {
		return AbstractBroadcastEventReader.TYPE_BROADCAST;
	}
	
	public AbstractBroadcastEventReader(Context c, PhoneConstants phoneValues, String mainDataId) {
		super(c, phoneValues, mainDataId);
		init();
	}
}