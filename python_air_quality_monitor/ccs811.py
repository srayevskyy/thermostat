# this example reads and prints CO2 equiv. measurement, TVOC measurement, and temp every 2 seconds

from Adafruit_CCS811 import Adafruit_CCS811
import Adafruit_GPIO.SPI as SPI
import Adafruit_SSD1306

import datetime
import socket
import fcntl
import struct

from PIL import Image
from PIL import ImageFont
from PIL import ImageDraw
from time import sleep


def get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
    )[20:24])

co2Value = 0
tvocValue = 0
tempValue = 0

ccs = Adafruit_CCS811()

while not ccs.available():
    pass

temp = ccs.calculateTemperature()
ccs.tempOffset = temp - 25.0

if ccs.available():
    while (co2Value == 0):
        tempValue = ccs.calculateTemperature()
        if not ccs.readData():
            co2Value = ccs.geteCO2()
            tvocValue = ccs.getTVOC()
        sleep(1)

disp = Adafruit_SSD1306.SSD1306_128_64(rst=24, i2c_bus=0)
disp.begin()
disp.clear()
disp.display()
image = Image.new('1', (disp.width, disp.height))
font = ImageFont.truetype('../fonts/red_alert.ttf', 14)
draw = ImageDraw.Draw(image)
draw.text((0, 0), "CO2: {} TVOC: {}".format(
    co2Value, tvocValue), font=font, fill=255)
draw.text((0, 16), "Temp: {} C".format(tempValue), font=font, fill=255)
draw.text((0, 32), "Last: {}".format(
    datetime.datetime.today().strftime("%b, %d %H:%M:%S")), font=font, fill=255)
draw.text((0, 48), "IP: {}".format(
    get_ip_address('wlan0')), font=font, fill=255)
disp.image(image)
disp.display()
