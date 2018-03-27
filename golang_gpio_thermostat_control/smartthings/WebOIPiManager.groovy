/**
 *  WebOIPi Manager
 *
 *  Copyright 2016 iBeech
 *
 *  Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
        name: "WebOIPi Manager",
        namespace: "ibeech",
        author: "ibeech",
        description: "Add each Pi Relay as an individual thing.",
        category: "Safety & Security",
        iconUrl: "http://download.easyicon.net/png/1179528/64/",
        iconX2Url: "http://download.easyicon.net/png/1179528/128/",
        iconX3Url: "http://download.easyicon.net/png/1179528/128/")

preferences {

    section("Thermostat") {
        input "deviceName1", "text", title: "Device Name", required: false, defaultValue: "00 Thermostat relay"
        input "piIP1", "text", "title": "Raspberry Pi IP", multiple: false, required: true, defaultValue: "192.168.86.23"
        input "piPort1", "text", "title": "Raspberry Pi Port", multiple: false, required: true, defaultValue: "8001"
        input "gpioNumber1", "text", title: "Raspberry Pi GPIO#", required: false, defaultValue: "21"
        input "deviceTimeStart1", "text", title: "Time relay ON (from)", required: false, defaultValue: "08:00"
        input "deviceTimeEnd1", "text", title: "Time relay ON (to)", required: false, defaultValue: "01:00"
        input "displayDevice1", "text", title: "OLED device name", required: false, defaultValue: "/dev/i2c-1"
    }

    section("Fireplace outlet") {
        input "deviceName2", "text", title: "Device Name", required: false, defaultValue: "00 Fireplace outlet"
        input "piIP2", "text", "title": "Raspberry Pi IP", multiple: false, required: true, defaultValue: "192.168.86.65"
        input "piPort2", "text", "title": "Raspberry Pi Port", multiple: false, required: true, defaultValue: "8001"
        input "gpioNumber2", "text", title: "Raspberry Pi GPIO#", required: false, defaultValue: "14"
        input "deviceTimeStart2", "text", title: "Time relay ON (from)", required: false, defaultValue: ""
        input "deviceTimeEnd2", "text", title: "Time relay ON (to)", required: false, defaultValue: ""
        input "displayDevice2", "text", title: "OLED device name", required: false, defaultValue: ""
    }

}

def installed() {
    log.debug "Installed with settings: ${settings}"

    /*
    def hub = location.hubs[0]
    log.debug "Hub id: ${hub.id}"
    log.debug "zigbeeId: ${hub.zigbeeId}"
    log.debug "zigbeeEui: ${hub.zigbeeEui}"
    log.debug "type: ${hub.type}" // PHYSICAL or VIRTUAL
    log.debug "name: ${hub.name}"
    log.debug "firmwareVersionString: ${hub.firmwareVersionString}"
    log.debug "localIP: ${hub.localIP}"
    log.debug "localSrvPortTCP: ${hub.localSrvPortTCP}"
    */

    initialize()
}

def initialize() {

    subscribe(location, null, response, [filterEvents: false])

    setupVirtualRelay((String) deviceName1, (String) piIP1, (String) piPort1, (String) gpioNumber1, (String) deviceTimeStart1, (String) deviceTimeEnd1, (String) displayDevice1)
    setupVirtualRelay((String) deviceName2, (String) piIP2, (String) piPort2, (String) gpioNumber2, (String) deviceTimeStart2, (String) deviceTimeEnd2, (String) displayDevice2)
}

def updated() {
    //log.debug "Updated with settings: ${settings}"

    //updateGPIOState()
    unsubscribe()

    updateVirtualRelay((String) deviceName1, (String) piIP1, (String) piPort1, (String) gpioNumber1, (String) deviceTimeStart1, (String) deviceTimeEnd1, (String) displayDevice1)
    updateVirtualRelay((String) deviceName2, (String) piIP2, (String) piPort2, (String) gpioNumber2, (String) deviceTimeStart2, (String) deviceTimeEnd2, (String) displayDevice2)

    subscribe(location, null, response, [filterEvents: false])
}

def updateVirtualRelay(
        String deviceName,
        String piIP,
        String piPort,
        String gpioNumber,
        String relayOnTimeStart,
        String relayOnTimeEnd,
        String displayDevice
) {

    // If user didn't fill this device out, skip it
    if (!deviceName) return

    String theDeviceNetworkId = getRelayID(piIP, piPort, gpioNumber)

    //log.trace "updateVirtualRelay: searching for: $theDeviceNetworkId"

    def d = getChildDevices().find { d -> d.deviceNetworkId.startsWith(theDeviceNetworkId) }

    if (d) { // The switch already exists
        //log.debug "Found existing device which we will now update"
        d.deviceNetworkId = theDeviceNetworkId + "." + gpioNumber
        d.label = deviceName
        d.name = deviceName

        subscribe(d, "switch", switchChange)
        /*
        log.debug "Setting initial state of $deviceName to off"
        setDeviceState(gpioNumber, "off", relayOnTimeStart, relayOnTimeEnd)
        d.off()
        */
    } else { // The switch does not exist
        if (deviceName) { // The user filled in data about this switch
            //log.debug "updateVirtualRelay: device '${deviceName}' does not exist, creating a new one now"
            setupVirtualRelay(deviceName, piIP, piPort, gpioNumber, relayOnTimeStart, relayOnTimeEnd, displayDevice)
        }
    }

}

def setupVirtualRelay(
        String deviceName,
        String piIP,
        String piPort,
        String gpioNumber,
        String deviceTimeStart,
        String deviceTimeEnd,
        String displayDevice
) {

    if (deviceName) {
        /*
        log.debug deviceName
        log.debug gpioNumber
        log.debug deviceTimeStart
        log.debug deviceTimeEnd
        */

        def theHub = location.hubs[0]

        //log.trace "Found a relay switch called $deviceName on GPIO #${gpioNumber}"
        def d = addChildDevice(
                "ibeech",
                "Virtual Pi Relay",
                getRelayID(piIP, piPort, gpioNumber),
                theHub.id,
                [label: deviceName, name: deviceName, completedSetup: true]
        )

        //d.sendEvent(name: "timeStart", value: deviceTimeStart)
        //d.sendEvent(name: "timeEnd", value: deviceTimeEnd)

        d.sendEvent(name: "timeStart", value: "08:00")
        d.sendEvent(name: "timeEnd", value: "01:01")

        subscribe(d, "switch", switchChange)

        /*
        log.debug "Setting initial state of $gpioName to off"
        setDeviceState(gpioNumber, "off")
        d.off()
        */

    }
}

static String getRelayID(String piIP, String piPort, String gpioNumber) {
    return "piRelay." + piIP + "." + piPort + "." + gpioNumber
}

def uninstalled() {
    // unsubscribe() -- deprecated
    def delete = getChildDevices()
    delete.each {
        //unsubscribe(it)
        //log.trace "about to delete device"
        deleteChildDevice(it.deviceNetworkId)
    }
}

def response(evt) {
    def msg = parseLanMessage(evt.description)
    if (msg && msg.body) {

        // This is the GPIO header state message
        def children = getChildDevices(false)
        if (msg.json) {
            msg.json.GPIO.each { item ->
                updateRelayDevice(item.key, item.value.value, children)
            }

            //log.trace "Finished Getting GPIO State"
        }
    }
}

static updateRelayDevice(GPIO, state, childDevices) {

    def theSwitch = childDevices.find { d -> d.deviceNetworkId.endsWith(".$GPIO") }
    if (theSwitch) {
        //log.debug "Updating switch $theSwitch for GPIO $GPIO with value $state"
        theSwitch.changeSwitchState(state)
    }
}

/*
def updateGPIOState() {

    //log.trace "Updating GPIO map"

    executeRequest("/*", "GET", false, null)

    runIn(10, updateGPIOState)
}
*/

def switchChange(evt) {

    log.debug "switchChange: switch event, value: ${evt.value}"

    if (evt.value == "on" || evt.value == "off") return

    String[] parts = evt.value.tokenize('.')
    String ipAddr = parts[1] + "." + parts[2] + "." + parts[3] + "." + parts[4]
    String ipPort = parts[5]
    String GPIO = parts[7]
    String state = parts[8]

    //log.debug "switchChange: state: ${state}"

    switch (state) {
        case "refresh":
            // Refresh this switches button
            //log.debug "Refreshing the state of GPIO " + GPIO
            executeRequest(ipAddr, ipPort, "/*", "GET", false, null)
            return
    }

    setDeviceState(ipAddr, ipPort, GPIO, state, (String) deviceTimeStart1, (String) deviceTimeEnd1, (String) displayDevice1)
}

def setDeviceState(
        String ipAddr,
        String ipPort,
        String gpioPin,
        String state,
        String timeStart,
        String timeEnd,
        String displayDevice
) {
    log.debug "Executing 'setDeviceState('${ipAddr}', '${ipPort}', '${gpioPin}', '${state}', '${timeStart}', '${timeEnd}, '${displayDevice}')"

    LinkedHashMap headers = [:]
    headers.put("HOST", "${ipAddr}:${ipPort}")

    String stateInt = (state == 'on' ? "1" : "0")

    String requestBody = "\"state\":\"${stateInt}\""

    try {

        if (timeStart) { requestBody += ", \"timestart\":\"${timeStart}\"" }
        if (timeEnd) { requestBody += ", \"timeend\":\"${timeEnd}\"" }
        if (displayDevice) { requestBody += ", \"displaydev\":\"${displayDevice}\"" }

        log.debug "Going to send request: " + requestBody

        physicalgraph.device.HubAction actualAction = new physicalgraph.device.HubAction(
                method: method,
                path: "/GPIO/" + gpioPin,
                headers: headers,
                body: "{" + requestBody + "}"
        )

        sendHubCommand(actualAction)
    }
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

def executeRequest(
        String ipAddr,
        String ipPort,
        String Path,
        String method,
        boolean setGPIODirection,
        String gpioPin
) {

    //log.debug "The " + method + " path is: " + Path

    LinkedHashMap headers = [:]
    headers.put("HOST", "${ipAddr}:${ipPort}")

    try {

        if (setGPIODirection) {
            physicalgraph.device.HubAction setDirection = new physicalgraph.device.HubAction(
                    method: "POST",
                    path: "/GPIO/" + gpioPin + "/function/OUT",
                    headers: headers)

            sendHubCommand(setDirection)
        }

        physicalgraph.device.HubAction actualAction = new physicalgraph.device.HubAction(
                method: method,
                path: Path,
                headers: headers)

        sendHubCommand(actualAction)
    }
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}
