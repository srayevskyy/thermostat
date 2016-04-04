#!/usr/bin/env python

import RPi.GPIO as GPIO

thermostat_relay_gpio_channel = 21

def main():
    GPIO.setmode(GPIO.BCM)
    GPIO.setwarnings(False)
    GPIO.setup(thermostat_relay_gpio_channel, GPIO.OUT)
    GPIO.output(thermostat_relay_gpio_channel, False)

if __name__ == "__main__":
    main()
