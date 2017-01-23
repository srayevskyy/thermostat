LOGDATEFORMAT='+%Y-%m-%d %H:%M:%S %p'

if ps axu | grep --silent "[s]mart_outlet_webserver.py"; then
  echo "`date \"${LOGDATEFORMAT}\"` INFO [${0}] Smart outlet webserver (smart_outlet_webserver.py) is already running, exiting"
  exit 0
fi

cd /home/pi/thermostat/smart_outlet_control/

sudo python smart_outlet_webserver.py "fireplace lamp" 52000
