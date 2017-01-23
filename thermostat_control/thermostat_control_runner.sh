LOGDATEFORMAT='+%Y-%m-%d %H:%M:%S %p'

if ps axu | grep --silent "[t]hermostat_control.py"; then
  echo "`date \"${LOGDATEFORMAT}\"` INFO [${0}] Thermostat control program (thermostat_control.py) is already running, exiting"
  exit 0
fi

cd /home/pi/thermostat/thermostat_control/

sudo python thermostat_control.py

