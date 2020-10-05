#!/bin/bash

set -e

LOGDATEFORMAT='+%Y-%m-%d %H:%M:%S %p'

if ps axu | grep --silent "[a]ir_quality_monitor.py"; then
  echo "`date \"${LOGDATEFORMAT}\"` INFO [${0}] Air quality monitor program (air_quality_monitor.py) is already running, exiting"
  exit 0
fi

cd /home/pi/src/github.com/srayevskyy/thermostat/python_air_quality_monitor
source ./venv/bin/activate
python air_quality_monitor.py

