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

package edu.umich.PowerTutor.phone;

import edu.umich.PowerTutor.dataReaders.Audio.AudioData;
import edu.umich.PowerTutor.dataReaders.CPU.CpuData;
import edu.umich.PowerTutor.dataReaders.GPS.GpsData;
import edu.umich.PowerTutor.dataReaders.LCD.LcdData;
import edu.umich.PowerTutor.dataReaders.OLED.OledData;
import edu.umich.PowerTutor.dataReaders.Sensors.SensorData;
import edu.umich.PowerTutor.dataReaders.Threeg.ThreegData;
import edu.umich.PowerTutor.dataReaders.Wifi.WifiData;

public interface PhonePowerCalculator {
  public double getLcdPower(LcdData data);

  public double getOledPower(OledData data);

  public double getCpuPower(CpuData data);

  public double getAudioPower(AudioData data);

  public double getGpsPower(GpsData data);

  public double getWifiPower(WifiData data);

  public double getThreeGPower(ThreegData data);

  public double getSensorPower(SensorData data);
}

