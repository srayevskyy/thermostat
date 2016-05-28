#KY040 Python Class
#Martin O'Hanlon
#stuffaboutcode.com

import RPi.GPIO as GPIO
from time import sleep

class KY040:

    CLOCKWISE = 0
    ANTICLOCKWISE = 1

    def __init__(self, clockPin, dataPin, switchPin, rotaryCallback, switchCallback):
        #persist values
        self.clockPin = clockPin
        self.dataPin = dataPin
        self.switchPin = switchPin
        self.rotaryCallback = rotaryCallback
        self.switchCallback = switchCallback

        #setup pins
        GPIO.setup(clockPin, GPIO.IN)
        GPIO.setup(dataPin, GPIO.IN)
        GPIO.setup(switchPin, GPIO.IN, pull_up_down=GPIO.PUD_UP)

    def start(self):
        GPIO.add_event_detect(self.clockPin, GPIO.BOTH, callback=self._clockCallback, bouncetime=80)
        GPIO.add_event_detect(self.switchPin, GPIO.BOTH, callback=self._switchCallback, bouncetime=100)

    def stop(self):
        GPIO.remove_event_detect(self.clockPin)
        GPIO.remove_event_detect(self.switchPin)

    def _clockCallback(self, pin):
        data_clock = GPIO.input(self.clockPin)
        data = GPIO.input(self.dataPin)
	print data_clock, data
	#TBD: Gray code needs to be implemented
        if data_clock == 0:
            if data == 1:
                self.rotaryCallback(self.ANTICLOCKWISE)
            else:
                self.rotaryCallback(self.CLOCKWISE)

    def _switchCallback(self, pin):
        self.switchCallback(GPIO.input(self.switchPin))

#test
if __name__ == "__main__":

    CLOCKPIN = 5
    DATAPIN = 6
    SWITCHPIN = 13

    def rotaryChange(direction):
        print "turned - " + str(direction)

    def switchEvent(state):
        print "button event: ", state

    GPIO.setmode(GPIO.BCM)

    ky040 = KY040(CLOCKPIN, DATAPIN, SWITCHPIN, rotaryChange, switchEvent)

    ky040.start()

    try:
        while True:
            sleep(0.1)
    finally:
        ky040.stop()
        GPIO.cleanup()
