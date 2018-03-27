#!/bin/bash

LOGDATEFORMAT='+%Y-%m-%d %H:%M:%S'

if ps axu | grep --silent "[g]pio_server.go"; then
  echo "`date \"${LOGDATEFORMAT}\"` INFO [${0}] GPIO server is already running, exiting"
  exit 0
fi

cd /home/pi/go/src/github.com/srayevskyy/thermostat/golang_gpio_thermostat_control

killall -q gpio_server

export PATH=$PATH:/usr/local/go/bin

nohup go run ./gpio_server.go >> gpio_server.log 2>&1 &
