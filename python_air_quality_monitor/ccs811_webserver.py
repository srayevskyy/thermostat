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

from PIL import Image
from PIL import ImageFont
from PIL import ImageDraw

TIMEFORMAT = '%b, %d %H:%M:%S'
BIND_ADDR = '0.0.0.0'
BIND_PORT = 5002

co2Value = 0
tvocValue = 0
tempValue = 0
lastTimeSensorRead = datetime.datetime.today()


def read_sensor_values():
    global co2Value, tvocValue, tempValue, lastTimeSensorRead

    ccs = Adafruit_CCS811()

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
        }
    ]

    SCHEDULER_API_ENABLED = True


class SensorValueByType(Resource):
    def get(self, valueType):
        global co2Value, tvocValue, tempValue, lastTimeSensorRead
        if valueType.upper() == 'CO2':
            result = {
                'CO2': co2Value, 'lastTimeSensorRead': lastTimeSensorRead.strftime(TIMEFORMAT)}
        elif valueType.upper() == 'TVOC':
            result = {'TVOC': tvocValue,
                      'lastTimeSensorRead': lastTimeSensorRead.strftime(TIMEFORMAT)}
        elif valueType.upper() == 'TEMP':
            result = {'Temp': round(
                tempValue, 1), 'lastTimeSensorRead': lastTimeSensorRead.strftime(TIMEFORMAT)}
        return jsonify(result)


class SensorValues(Resource):
    def get(self):
        global co2Value, tvocValue, tempValue, lastTimeSensorRead
        result = {'CO2': co2Value, 'TVOC': tvocValue, 'Temp': round(
            tempValue, 1), 'lastTimeSensorRead': lastTimeSensorRead.strftime(TIMEFORMAT)}
        return jsonify(result)


if __name__ == '__main__':
    app = Flask(__name__)
    api = Api(app)
    api.add_resource(SensorValues, '/sensor_values')
    api.add_resource(SensorValueByType, '/sensor_value_by_type/<valueType>')
    app.config.from_object(Config())

    # initial read of sensor values
    read_sensor_values()

    scheduler = APScheduler()
    scheduler.init_app(app)
    scheduler.start()
    app.run(host=BIND_ADDR, port=BIND_PORT)
