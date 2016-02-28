This is a project to override control of a home thermostat with Raspbery Pi.

#### Initial setup

```sudo apt-get install php5 git i2c-tools```

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
```gpio readall```

#### add crontab entry
```crontab -e```

#### add the following entry to users crontab
```* * * * * /usr/bin/php /home/pi/thermostat/thermostat_control.ph```

## Adding a hardware clock to Raspberry Pi (DS3231)
#### list devices on i2c bus
```sudo i2cdetect -y 0 #for Raspberry Pi Model B```
```sudo i2cdetect -y 1 #for Raspberry Pi Zero```
#### Comment out the blacklist entry so the module can be loaded on boot 
```sudo sed -i 's/blacklist i2c-bcm2708/#blacklist i2c-bcm2708/' /etc/modprobe.d/raspi-blacklist.conf```
#### Load the module now
```sudo modprobe i2c-bcm2708```
#### Notify Linux of the Dallas RTC device (use -0 for Model A or -1 for Model B)
```echo ds3231 0x68 | sudo tee /sys/class/i2c-adapter/i2c-1/new_device```
#### Test whether Linux can see our RTC module.
```sudo hwclock```
You should see a response with what the chip thinks is the date.
#### Set system date
```sudo date -s "Sep 27 2014 12:46:00"```
#### Now that your system date is correct, simply use this command to transfer the system date to the chip:
```sudo hwclock -w```
#### Next, to read the time from the RTC chip and set the system time from it at every boot, open /etc/rc.local and add these two lines above the exit 0 line:
```
echo ds3231 0x68 > /sys/class/i2c-adapter/i2c-0/new_device
hwclock -s
```
#### We can also disable the ntp daemon and fake-hwclock during boot
```
sudo update-rc.d ntp disable
sudo update-rc.d fake-hwclock disable
```
#### You can still sync the system time from the internet using
```
sudo ntpd -gq
sudo hwclock -w
```
