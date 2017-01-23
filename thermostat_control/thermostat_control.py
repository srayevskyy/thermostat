#!/usr/bin/env python

import socket
import fcntl
import struct
import datetime
import RPi.GPIO as GPIO
from oled.device import ssd1306
from oled.render import canvas
from PIL import ImageDraw, ImageFont

oled_port = 1
oled_address = 0x3C
thermostat_relay_gpio_channel = 21

# Time (Hour and minute) when thermostat circuit should be ON
daytime_start_hours = 8
daytime_start_minutes = 0

# Duration of 'daytime' period in hours and minutes, when a thermostat circuit should be ON
daytime_duration_hours = 17
daytime_duration_minutes = 00

def get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
    )[20:24])

def make_decision(oled):
    # set default value
    decision = "off"
    # get current time
    ct = datetime.datetime.now()
    # construct 'interval'
    delta = datetime.timedelta(hours = daytime_duration_hours, minutes = daytime_duration_minutes)
    # derive today's and yesterday's start times
    dt_begin_today = datetime.datetime(ct.year, ct.month, ct.day, daytime_start_hours, daytime_start_minutes, 0)
    dt_begin_yesterday = datetime.datetime(ct.year, ct.month, ct.day, daytime_start_hours, daytime_start_minutes, 0) - datetime.timedelta(days=1)
    # derive today's and yesterday's end times
    dt_end_today = dt_begin_today + delta
    dt_end_yesterday = dt_begin_yesterday + delta

    # the most important part - here a decision to turn on thermostat is made
    if ((ct >= dt_begin_today) and (ct <= dt_end_today)) or ((ct >= dt_begin_yesterday) and (ct <= dt_end_yesterday)):
        decision = "on"

    # initialize fonts for render on the screen
    font = ImageFont.load_default()
    font2 = ImageFont.truetype('../fonts/red_alert.ttf', 14)
    font3 = ImageFont.truetype('../fonts/red_alert.ttf', 20)

    # display status information on the screen
    with canvas(oled) as draw:
        # display current decision (ON or OFF)
        draw.text((0, 0), decision.upper(), font=font3, fill=255)
        # display last time when thermostat control script was executed
        draw.text((0, 18), ct.strftime('LAST: %y/%m/%d %H:%M'), font=font2, fill=255)
        # show time interval when thermostat should be ON
        draw.text((0, 32), 'When ON: ' + dt_begin_today.strftime('%H:%M') + ' - ' + dt_end_today.strftime('%H:%M'), font=font2, fill=255)
        # show IP address of wifi adapter
        draw.text((0, 46), "IP: " + get_ip_address('wlan0'), font=font2, fill=255)

    # send the command to relay using GPIO pins on raspberry
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
