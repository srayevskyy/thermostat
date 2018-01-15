#!/bin/bash

LOGDATEFORMAT='+%Y-%m-%d %H:%M:%S %p'

if ps axu | grep --silent "[p]izero-camera-downloader.go"; then
  echo "`date \"${LOGDATEFORMAT}\"` INFO [${0}] downloader already running, exiting"
  exit 0
fi

cd /home/pi/src/thermostat/pizero-camera-downloader/cmd/pizero-camera-downloader

killall -q pizero-camera-downloader

export PATH=$PATH:/usr/local/go/bin

nohup go run pizero-camera-downloader.go >& pizero-camera-downloader.log &
