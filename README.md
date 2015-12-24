This is a project to override control of a home thermostat with Raspbery Pi.

Initial setup hints:

sudo apt-get install php5 git i2c-tools

#### get supplementary library wiringPI

```
cd ~
git clone git://git.drogon.net/wiringPi
cd wiringPi
./build
```

#### clone thermostat control project

```
cd ~
git clone https://github.com/srayevskyy/thermostat
cd thermostat
```

#### check GPIO outputs
```
gpio readall
```

#### add crontab entry
```
crontab -e
```

#### add the following entry to users crontab
```
* * * * * /usr/bin/php /home/pi/thermostat/thermostat_control.ph
```

