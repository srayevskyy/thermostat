#!/bin/bash

LOGDATEFORMAT='+%Y-%m-%d %H:%M:%S'

if ps axu | grep --silent "[d]irectory_web_server.go"; then
  echo "`date \"${LOGDATEFORMAT}\"` INFO [${0}] downloader already running, exiting"
  exit 0
fi

cd /home/pi/src/thermostat/directory_web_server/cmd/directory_web_server

killall -q directory_web_server

export PATH=$PATH:/usr/local/go/bin

nohup go run directory_web_server.go >> directory_web_server.log 2>&1 &
