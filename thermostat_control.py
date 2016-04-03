#!/usr/bin/env python

import os
import sys

if os.name != 'posix':
    sys.exit('platform not supported')

import psutil
import datetime
import RPi.GPIO as GPIO
from oled.device import ssd1306
from oled.render import canvas
from PIL import ImageDraw, ImageFont

oled_port = 1
oled_address = 0x3C
thermostat_relay_gpio_channel = 21
decision = "off"

# Time (Hour and minute) when thermostat circuit should be ON
daytime_start_hours = 8
daytime_start_minutes = 0

# Duration of 'daytime' period in hours and minutes, when a thermostat circuit should be ON
daytime_duration_hours = 17
daytime_duration_minutes = 0

def bytes2human(n):
    """
    >>> bytes2human(10000)
    '9K'
    >>> bytes2human(100001221)
    '95M'
    """
    symbols = ('K', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y')
    prefix = {}
    for i, s in enumerate(symbols):
        prefix[s] = 1 << (i+1)*10
    for s in reversed(symbols):
        if n >= prefix[s]:
            value = int(float(n) / prefix[s])
            return '%s%s' % (value, s)
    return "%sB" % n

def network(iface):
    stat = psutil.net_io_counters(pernic=True)[iface]
    return "%s: Tx%s, Rx%s" % \
           (iface, bytes2human(stat.bytes_sent), bytes2human(stat.bytes_recv))

def make_decision(oled):

    # TBD: need to make a decision - on or off
    current_time = datetime.datetime.now()

    # refresh data on device display
    font = ImageFont.load_default()
    font2 = ImageFont.truetype('fonts/red_alert.ttf', 14)
    font3 = ImageFont.truetype('fonts/red_alert.ttf', 20)
    with canvas(oled) as draw:
        draw.text((0, 0), decision.upper(), font=font3, fill=255)
        draw.text((0, 18), current_time.strftime('%Y-%m-%d %H:%M:%S'), font=font2, fill=255)
        draw.text(
          (0, 32)
          , ("0" + str(daytime_start_hours))[:2] + ':' + ("0" + str(daytime_start_minutes))[:2] + ' ``('
            + ("0" + str(daytime_duration_hours))[:2] + ':' + ("0" + str(daytime_duration_minutes))[:2]
            + ')'
          , font=font2, fill=255
        )
        draw.text((0, 46), network('wlan0'), font=font2, fill=255)

    # send the command to relay
    GPIO.setmode(GPIO.BCM)
    GPIO.setwarnings(False)
    GPIO.setup(thermostat_relay_gpio_channel, GPIO.OUT)
    if decision == 'on':
        GPIO.output(thermostat_relay_gpio_channel, True)
    else:
        GPIO.output(thermostat_relay_gpio_channel, False)

def main():
    oled = ssd1306(port=oled_port, address=oled_address)
    make_decision(oled)

if __name__ == "__main__":
    main()
