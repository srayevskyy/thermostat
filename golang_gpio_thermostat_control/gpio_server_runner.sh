#!/bin/bash

LOGDATEFORMAT='+%Y-%m-%d %H:%M:%S %p'

if ps axu | grep --silent "[g]pio_server.go"; then
  echo "`date \"${LOGDATEFORMAT}\"` INFO [${0}] GPIO server is already running, exiting"
  exit 0
fi

cd /home/pi/thermostat/golang_gpio_thermostat_control

killall -q gpio_server

export PATH=$PATH:/usr/local/go/bin

nohup go run /home/pi/thermostat/golang_gpio_thermostat_control/gpio_server.go >& gpio_server.log &
