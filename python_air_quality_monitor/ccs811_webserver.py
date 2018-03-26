from flask import Flask, request
from flask_restful import Resource, Api
from json import dumps
from flask_jsonpify import jsonify
from flask_apscheduler import APScheduler
from Adafruit_CCS811 import Adafruit_CCS811
import Adafruit_GPIO.SPI as SPI
import Adafruit_SSD1306
import time
import datetime
import socket
import fcntl
import struct

from PIL import Image
from PIL import ImageFont
from PIL import ImageDraw

TIMEFORMAT = '%b, %d %H:%M:%S'
BIND_ADDR = '0.0.0.0'
BIND_PORT = 5002
I2C_BUS_OLED = 0
I2C_BUS_CCS811 = 1

co2Value = 0
tvocValue = 0
tempValue = 0
lastTimeSensorRead = datetime.datetime.today()


def get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
    )[20:24])


def display_sensor_values():
    global co2Value, tvocValue, tempValue, lastTimeSensorRead
    disp = Adafruit_SSD1306.SSD1306_128_64(rst=24, i2c_bus=I2C_BUS_OLED)
    disp.begin()
    disp.clear()
    disp.display()
    image = Image.new('1', (disp.width, disp.height))
    font = ImageFont.truetype('../fonts/red_alert.ttf', 14)
    draw = ImageDraw.Draw(image)
    draw.text((0, 0), "CO2: {} TVOC: {}".format(
        co2Value, tvocValue), font=font, fill=255)
    draw.text((0, 16), "Temp: {} C".format(tempValue), font=font, fill=255)
    draw.text((0, 32), "Last: {}".format(
        lastTimeSensorRead.strftime(TIMEFORMAT)), font=font, fill=255)
    draw.text((0, 48), "IP: {}".format(
        get_ip_address('wlan0')), font=font, fill=255)
    disp.image(image)
    disp.display()


def read_sensor_values():
    global co2Value, tvocValue, tempValue, lastTimeSensorRead

    ccs = Adafruit_CCS811(busnum=I2C_BUS_CCS811)

    while not ccs.available():
        pass

    tempValue = ccs.calculateTemperature()
    ccs.tempOffset = tempValue - 25.0

    if ccs.available():
        while (co2Value == 0):
            tempValue = ccs.calculateTemperature()
            if not ccs.readData():
                co2Value = ccs.geteCO2()
                tvocValue = ccs.getTVOC()
            time.sleep(1)

    lastTimeSensorRead = datetime.datetime.today()


class Config(object):
    JOBS = [
        {
            'id': 'read_sensor_values',
            'func': read_sensor_values,
            'trigger': 'interval',
            'seconds': 20
        },
        {
            'id': 'display_sensor_values',
            'func': display_sensor_values,
            'trigger': 'interval',
            'seconds': 30
        },
    ]

    SCHEDULER_API_ENABLED = True


class SensorValueByType(Resource):
    def get(self, valueType):
        global co2Value, tvocValue, tempValue, lastTimeSensorRead
        if valueType.upper() == 'CO2':
            result = {
                'co2': co2Value, 'lastTimeSensorRead': lastTimeSensorRead.strftime(TIMEFORMAT)}
        elif valueType.upper() == 'TVOC':
            result = {'tvoc': tvocValue,
                      'lastTimeSensorRead': lastTimeSensorRead.strftime(TIMEFORMAT)}
        elif valueType.upper() == 'TEMP':
            result = {'temp': round(
                tempValue, 1), 'lastTimeSensorRead': lastTimeSensorRead.strftime(TIMEFORMAT)}
        return jsonify(result)


class SensorValues(Resource):
    def get(self):
        global co2Value, tvocValue, tempValue, lastTimeSensorRead
        result = {'co2': co2Value, 'tvoc': tvocValue, 'temp': round(
            tempValue, 1), 'lastTimeSensorRead': lastTimeSensorRead.strftime(TIMEFORMAT)}
        return jsonify(result)


if __name__ == '__main__':
    app = Flask(__name__)
    api = Api(app)
    api.add_resource(SensorValues, '/sensor_values')
    api.add_resource(SensorValueByType, '/sensor_value_by_type/<valueType>')
    app.config.from_object(Config())

    # initial read and display of sensor values
    read_sensor_values()
    display_sensor_values()

    scheduler = APScheduler()
    scheduler.init_app(app)
    scheduler.start()
    app.run(host=BIND_ADDR, port=BIND_PORT)
