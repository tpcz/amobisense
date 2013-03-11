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
*/

package cz.cuni.mff.d3s.Amobisense.context;

import android.util.Log;
import cz.cuni.mff.d3s.Amobisense.utils.Recycler;

/* 
 * Something like "dynamic type or bundle", 
 * encapsulates long, doubles or string read data. 
 */
public class ContextData implements Comparable<ContextData> {
	
    
	private static Recycler<ContextData> recycler = new Recycler<ContextData>();
	private static String TAG = "ContextData";

	public ContextData() {

	}

	public void recycle() {
		recycler.recycle(this);
	}
  
    private long     lvalue = 0L;
    private double   dvalue = 0.0D;
	private StringBuilder svalue = new StringBuilder("");
	
	public ContextType dataType = ContextType.NOT_SET;

	public void setValue(int value) {
		setValue((long)value);
	}
	
	public void setValue(float value) {
		setValue((double)value);
	}
	
	public ContextData (String value) {
		setValue(value);
	}
	
	public void setValue (long value) {
		lvalue = value;
		dvalue = 0;
		svalue.setLength(0);
		dataType = ContextType.LONG;
	}
	
	public void incrementByOne () {
		if (this.dataType != ContextType.LONG) {
			Log.e(TAG, "Cannot increment non long value");
			return;
		}
		
		lvalue++;
	}
	
	public void setValue (double value) {
		lvalue = 0;
		svalue.setLength(0);
		dvalue = value;
		dataType = ContextType.DOUBLE;
	}
	
	public void setValue (String value) {
		svalue.setLength(0);
		svalue.append(value);
		lvalue = 0;
		dvalue = 0;
		dataType = ContextType.STRING;
	}
	
	public void appendStringValue (String value, boolean clear) {
		if (clear) {
			svalue.setLength(0);
		}
		svalue.append(value);
	} 
	
	
	public void setValue (ContextData value) {
		 
			switch  (value.dataType) {
			case NOT_SET:
				this.dataType = ContextType.NOT_SET;
			break;
			case LONG:
				setValue(value.toLong());
			break;
			case DOUBLE:
				setValue(value.toDouble());
				break;
			case STRING:
				setValue(value.toString());
				break;
			case MIXED_LNUM_STRING:
				setValue(value.toString(), value.toLong());
				break;
			}
	}
	
	public void setValue (String svalue, long lvalue) {
		this.svalue.setLength(0);
		this.svalue.append(svalue);
		this.lvalue = lvalue;
		this.dataType = ContextType.MIXED_LNUM_STRING;
	}
	
	
	
	@Override
	public String toString() {
		switch  (dataType) {
		case NOT_SET:
			return "NOT SET";
		case LONG:
			return "" + lvalue;
		case DOUBLE:
			return "" + dvalue;
		case STRING:
			return svalue.toString();
		case MIXED_LNUM_STRING:
			return svalue.toString();
		}
		return "";
	}
	
	/** this function is here only because of frequent log writes... 
	 * 
	 * SHOULD NOT BE USED IN OTHER CODE!
	 * 
	 * */
	public StringBuilder __getStringBuilderRefUnchcked () {
		return svalue; 
	}
	
	public Boolean toBool() {
		switch  (dataType) {
		case NOT_SET:
			return false;
		case LONG:
		case DOUBLE:
		case MIXED_LNUM_STRING:
			return (lvalue != 0 || Math.abs(dvalue) > 0.00001);
		case STRING:
			return ! (svalue.length() == 0);
		}
		return false;
	}
	
	public long toLong() {
		switch  (dataType) {
		case NOT_SET:
			return -1;
		case DOUBLE:
			return Math.round(dvalue);
		case LONG:
		case MIXED_LNUM_STRING:
			return lvalue;
		case STRING:
			return -1;
		}
		return -1;
	}
	
	public double toDouble() {
		switch  (dataType) {
		case NOT_SET:
			return -1;
		case DOUBLE:
			return dvalue;
		case MIXED_LNUM_STRING:
		case LONG:
			return lvalue;	
		case STRING:
			return -1;
		}
		
		return -1;
	}
	
	public String getDetailDescription () {
		
		return this.toString();
	}
	
	/**
	 * MIXED value is compared according to number, if the numeric part is same, strings are compared.
	 */
	@Override
	public int compareTo(ContextData another) {
		if (another == null) {
			Log.e (TAG, "ContextData Compare to: another is NULL!");
			return -1;
		}
		
		if (dataType != another.dataType) {
			Log.e (TAG, "ContextData Compare to: Data type is different! " + dataType.toString() + " vs. " + another.dataType.toString());
			return -1;
		}
			
		switch  (dataType) {
		case NOT_SET:
			return 0;
		case DOUBLE:
			 if (this.dvalue > another.dvalue) {return 1;} else if (this.dvalue < another.dvalue) {return -1;}; return 0;
		case LONG:
			if (this.lvalue > another.lvalue) {return 1;} else if (this.lvalue < another.lvalue) {return -1;}; return 0;
		case MIXED_LNUM_STRING:
			if (this.lvalue > another.lvalue) {return 1;} else if (this.lvalue < another.lvalue) {return -1;};
			return this.toString().compareTo(another.toString());
		case STRING:
			this.svalue.toString().compareTo(another.svalue.toString());
		default:
			return 0;
		}
	}
	
	public boolean isIntegerType() {
		return 
				dataType == ContextType.LONG || 
				dataType == ContextType.INT;
	}
	
	public boolean isPoorNumericType () {
		return 
				dataType == ContextType.LONG   ||
				dataType == ContextType.INT    || 
				dataType == ContextType.DOUBLE ||
				dataType == ContextType.FLOAT;
	}
} 