LOGDATEFORMAT='+%Y-%m-%d %H:%M:%S %p'

if ps axu | grep --silent "[c]cs811.py"; then
  echo "`date \"${LOGDATEFORMAT}\"` INFO [${0}] Air quality monitor program (ccs811.py) is already running, exiting"
  exit 0
fi

cd /home/pi/go/src/github.com/srayevskyy/thermostat/python_air_quality_monitor
source ./venv/bin/activate
python ./ccs811.py

