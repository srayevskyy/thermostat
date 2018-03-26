from flask import Flask, request
from flask_restful import Resource, Api
from json import dumps
from flask_jsonpify import jsonify

from Adafruit_CCS811 import Adafruit_CCS811
import Adafruit_GPIO.SPI as SPI
import Adafruit_SSD1306
import time
import datetime

app = Flask(__name__)
api = Api(app)

def get_sensor_measurements():
    co2Value = 0
    tvocValue = 0
    tempValue = 0

    ccs = Adafruit_CCS811()

    while not ccs.available():
        pass

    temp = ccs.calculateTemperature()
    ccs.tempOffset = temp - 25.0

    if ccs.available():
        while (co2Value == 0):
            tempValue = ccs.calculateTemperature()
            if not ccs.readData():
                co2Value = ccs.geteCO2()
                tvocValue = ccs.getTVOC()
            time.sleep(1)

    return (co2Value, tvocValue, tempValue)

class SensorValueByType(Resource):
    def get(self, valueType):
        co2Value, tvocValue, tempValue = get_sensor_measurements()
        timeNow = datetime.datetime.today().strftime("%b, %d %H:%M:%S")
        if valueType.upper() == 'CO2':
            result = {'CO2': co2Value, 'Time': timeNow}
        elif valueType.upper() == 'TVOC':
            result = {'TVOC': tvocValue, 'Time': timeNow}
        elif valueType.upper() == 'TEMP':
            result = {'Temp': round(tempValue, 1), 'Time': timeNow}
        return jsonify(result)


class SensorValues(Resource):
    def get(self):
        co2Value, tvocValue, tempValue = get_sensor_measurements()
        result = {'CO2': co2Value, 'TVOC': tvocValue, 'Temp': round(tempValue, 1), 'Time': datetime.datetime.today().strftime("%b, %d %H:%M:%S")}
        return jsonify(result)

api.add_resource(SensorValues, '/sensor_values')
api.add_resource(SensorValueByType, '/sensor_value_by_type/<valueType>')

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5002)
