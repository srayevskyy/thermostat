#!/bin/bash

LOGDATEFORMAT='+%Y-%m-%d %H:%M:%S'

camera_ip=192.168.1.254

if ! ping -Iwlan1 -c1 ${camera_ip}; then
  echo "`date \"${LOGDATEFORMAT}\"` [${0}] Warning: cannot reach camera by ip ${camera_ip}, trying to restart network interface"
  sudo ifconfig wlan1 down && sudo ifconfig wlan1 up && sleep 10
fi

if ps axu | grep --silent "[p]izero-camera-downloader.go"; then
  echo "`date \"${LOGDATEFORMAT}\"` INFO [${0}] downloader already running, exiting"
  exit 0
fi

cd /home/pi/src/thermostat/pizero-camera-downloader/cmd/pizero-camera-downloader

killall -q pizero-camera-downloader

export PATH=$PATH:/usr/local/go/bin

nohup go run pizero-camera-downloader.go >> pizero-camera-downloader.log 2>&1 &
