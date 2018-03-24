This is a project to override control of a home thermostat with Raspbery Pi.

### Initial setup

#### Install Raspbian Jessie Lite from https://www.raspberrypi.org/downloads/raspbian

#### pre-configure steps with raspi-config
```
sudo raspi-config
```
In the program:
- change user password
- set host name
- set memory split
- set 'US' keyboard layout
- expand filesystem

#### Open the wpa-supplicant configuration file in nano
```
sudo nano /etc/wpa_supplicant/wpa_supplicant.conf
```
Go to the bottom of the file and add the following: 
```
network={
    ssid="your_wifi_ssid"
    psk="Your_wifi_password"
}
```
Bounce wifi adapter:
```
sudo ifdown wlan0; sudo ifup wlan0; ip addr
```

#### Regenerate ssh keys
```
sudo rm -fv /etc/ssh/ssh_host_*
sudo dpkg-reconfigure openssh-server
```
#### Set up passwordless ssh

Create ssh keys directory
```
cd ~ && mkdir .ssh && chmod 700 ~/.ssh
```
Transfer your public key to Raspberr Pi.
NOTE: Run this from your computer
```
cat ~/.ssh/id_rsa.pub | ssh pi@<hostname> 'cat - >> ~/.ssh/authorized_keys; chmod 600 ~/.ssh/authorized_keys'
```

#### Change timezone
`sudo ln -sf /usr/share/zoneinfo/America/Los_Angeles /etc/localtime`

#### Install pre-requisites

```
sudo apt-get update && sudo apt-get -y upgrade
sudo apt-get install -y git i2c-tools golang-glide libjpeg-dev python-dev python-smbus python-pip
cd /tmp
wget https://storage.googleapis.com/golang/go1.10.linux-armv6l.tar.gz
sudo tar -C /usr/local -xvf go1.10.linux-armv6l.tar.gz
cat >> ~/.bashrc << 'EOF'
export GOPATH=$HOME/go
export PATH=/usr/local/go/bin:$PATH:$GOPATH/bin
EOF
source ~/.bashrc
rm -fv go1.10.linux-armv6l.tar.gz
```
#### Create directory for source code
`mkdir ~/src`

#### Install supplementary library WiringPi ('gpio' shell command)
```
cd ~/src
git clone https://github.com/WiringPi/WiringPi
cd ~/src/WiringPi/
./build
```
#### Check GPIO pins availability
`gpio readall`

#### Install supplementary library ssd1306 (OLED driver)

```
cd ~/src
git clone https://github.com/rm-hull/ssd1306
cd ~/src/ssd1306
sudo python setup.py install
```

#### Clone thermostat control project

```
cd ~/src
git clone https://github.com/srayevskyy/thermostat
```

#### Install pip dependencies
```
cd ~/src/thermostat
sudo pip install -U setuptools
sudo pip install -r requirements.txt 
```

#### add new entry to user's crontab
`crontab -e`

#### add the following entry to users crontab
##### For thermostat control
`* * * * * /home/pi/src/thermostat/thermostat_control/thermostat_control_runner.sh`
##### For smart outlet
`* * * * * /home/pi/src/thermostat/smart_outlet_control/smart_outlet_runner.sh`

### Adding a hardware clock (Dallas DS3231) to Raspberry Pi
#### list devices on i2c bus
```
sudo i2cdetect -y 0 #for Raspberry Pi Model B
sudo i2cdetect -y 1 #for Raspberry Pi Zero
```
#### Comment out the blacklist entry so the module can be loaded on boot 
`sudo sed -i 's/blacklist i2c-bcm2708/#blacklist i2c-bcm2708/' /etc/modprobe.d/raspi-blacklist.conf`
#### Load the kernel module
`sudo modprobe i2c-bcm2708`
#### Notify Linux of the Dallas RTC device (use -0 for Model A or -1 for Model B)
```
echo ds3231 0x68 | sudo tee /sys/class/i2c-adapter/i2c-0/new_device # for raspberry pi model b
echo ds3231 0x68 | sudo tee /sys/class/i2c-adapter/i2c-1/new_device # for raspberry pi zero
```
#### Test whether Linux can see our RTC module
`sudo hwclock`

Output: a response with datetime extracted from RTC chip.
#### Set system date
`sudo date -s "Sep 27 2014 12:46:00"`
#### Transfer the system date to the chip:
`sudo hwclock -w`
#### To read the time from the RTC chip and set the system time from it at every boot, open /etc/rc.local and add these two lines above the exit 0 line:
```
echo ds3231 0x68 | sudo tee /sys/class/i2c-adapter/i2c-0/new_device # for raspberry pi model b
# or
echo ds3231 0x68 | sudo tee /sys/class/i2c-adapter/i2c-1/new_device # for raspberry pi zero
/sbin/hwclock  -s
```
#### Disable the ntp daemon and fake-hwclock during boot
```
sudo update-rc.d ntp disable
sudo update-rc.d fake-hwclock disable
```
#### Sync the system time from the internet using (if needed)
```
sudo ntpd -gq
sudo hwclock -w
```
