DIR_NAME = '/var/www/amobisenseuploads/uploads/'  
import glob


import glob
DIR_NAME = '/var/www/amobisenseuploads/uploads/'  

def listDevices():

  files = glob.glob(DIR_NAME + "context*.log")

  individualDevices = []

  for file in files:
  
    parts = file.split('-')
    deviceId = parts[2]
  
    if not deviceId in individualDevices:
      individualDevices.append(deviceId)
  
    #print file + ' -> ' + deviceId
  
  for file in individualDevices:
    print file
    
  return individualDevices
