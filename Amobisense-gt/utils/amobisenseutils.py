import glob
DIR_NAME = '/var/www/amobisenseuploads/uploads/'  



"""Lists all devices that we have files from"""
def listDevices(printFiles=None):

  if printFiles is None:
    printFiles = False
  else:
    printFiles = True
  files = glob.glob(DIR_NAME + "context*.log")

  individualDevices = []

  for file in files:
  
    parts = file.split('-')
    deviceId = parts[2]
  
    if not deviceId in individualDevices:
      individualDevices.append(deviceId)
    
  if (printFiles) :
    for file in individualDevices:
      print file
    
  return individualDevices

  
def getDirName ():
    return DIR_NAME