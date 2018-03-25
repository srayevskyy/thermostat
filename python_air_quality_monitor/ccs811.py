# this example reads and prints CO2 equiv. measurement, TVOC measurement, and temp every 2 seconds

from time import sleep
from Adafruit_CCS811 import Adafruit_CCS811

# initialize fonts for render on the screen
#font = ImageFont.load_default()

ccs = Adafruit_CCS811()

while not ccs.available():
    pass

temp = ccs.calculateTemperature()
ccs.tempOffset = temp - 25.0

co2Value = 0
tvocValue = 0
tempValue = 0

if ccs.available():
    while (co2Value == 0):
        tempValue = ccs.calculateTemperature()
        if not ccs.readData():
            co2Value = ccs.geteCO2()
            tvocValue = ccs.getTVOC()
        sleep(1)

print "CO2: ", co2Value, "TVOC: ", tvocValue, "temp: ", tempValue
