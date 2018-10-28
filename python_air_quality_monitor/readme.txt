# Install from scratch

sudo apt-get update && sudo apt-get -y upgrade
sudo apt-get -y install git mc python-pip libjpeg-dev i2c-tools
echo 'pi:changeMe' | sudo chpasswd
pip install virtualenv
mkdir /home/pi/src
git clone https://github.com/srayevskyy/thermostat /home/pi/src/thermostat
cd /home/pi/src/thermostat/python_air_quality_monitor
/home/pi/.local/bin/virtualenv venv
source ./venv/bin/activate
pip install -r requirements.txt
echo -e '\n# Enable i2c-0 bus\ndtparam=i2c_vc=on\n \n# Set i2c baudrate\ndtparam=i2c_baudrate=10000' | sudo tee -a /boot/config.txt
# Add the following line to crontab
# * * * * * /home/pi/src/thermostat/python_air_quality_monitor/air_quality_monitor_runner.sh
