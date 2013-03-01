#! /usr/bin/python

import sys
import amobisenseutils
import glob
import datetime
import numpy as np
from numpy import *
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import matplotlib.mlab as mlab
import matplotlib.cbook as cbook


def mergeFiles(files, outputName):
  
  output = open(outputName, 'w')
  firstFile = True;
  times  = []
  timevalues = [];
  incompleteTimes = [];
  
  for file in files:
    input = open(file, 'r');
    if not firstFile:
      input.readline(); # skip first line
      input.readline(); # skip second line
    else:
      firstFile = False;
    while 1:
      line = input.readline()
      values = line.split(";")
      if (values[0].isdigit()):
	#times.append(datetime.datetime.fromtimestamp( int(values[0]) / 1000).strftime('%Y-%m-%d %H:%M:%S'))
	#times.append(int(values[0]) / 1000)
	times.append(datetime.datetime.fromtimestamp( int(values[0]) / 1000))
	timevalues.append(int(values[0]) / 1000)
      if not line:
        break
      output.write(line) 
  
  times.sort();
  timevalues.sort();
  
  previous = timevalues[0];
  for t in timevalues:  
    if (t - previous) > 15 :
      incompleteTimes.append(datetime.datetime.fromtimestamp(t))
    previous = t;
  
  for time in times:
   print time;

  def format_date(x, pos=None):
    thisind = np.clip(int(x+0.5), 0, N-1)
    return r.date[thisind].strftime('%Y-%m-%d %H:%M:%S')
   
  fig = plt.figure()
  ax = fig.add_subplot(111)
  o = ones ((len(times), 1))
  
  print size(o)
  print size(times)
  ax.plot( times, o, 'g.')
  ax.plot( incompleteTimes, ones((len(incompleteTimes), 1)), 'r.')
  fig.autofmt_xdate()
  plt.show()
  

if len( sys.argv ) <> 2: 
  print "Expecting StrinID of the device to merge...";
  print "Available devices:"
  amobisenseutils.listDevices()
else:
  deviceID  = sys.argv[1];
  files = glob.glob(amobisenseutils.getDirName() + "context-log-" + deviceID + "*.log")
  if len (files) == 0 :
    print "No such device";
  else:
    print "Merging files for device: " +  deviceID + "...";
    mergeFiles (files, "merged-" + deviceID)