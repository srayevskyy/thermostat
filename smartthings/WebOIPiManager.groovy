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

    section("Raspberry Pi Setup") {
        input "piIP", "text", "title": "Raspberry Pi IP", multiple: false, required: true, defaultValue: "192.168.86.23"
        input "piPort", "text", "title": "Raspberry Pi Port", multiple: false, required: true, defaultValue: "8001"
        input "theHub", "hub", title: "On which hub?", multiple: false, required: true
    }

    section("Device 1") {
        input "deviceName1", "text", title: "Device Name", required: false, defaultValue: "Thermostat relay"
        input "gpioNumber1", "text", title: "GPIO#", required: false, defaultValue: "21"
        input "deviceTimeStart1", "text", title: "Time relay ON (from)", required: false, defaultValue: "08:00"
        input "deviceTimeEnd1", "text", title: "Time relay ON (to)", required: false, defaultValue: "01:00"
    }

    /*
    section("Device 2") {
        input "deviceName1", "text", title: "Device Name", required: false, defaultValue: "Pi relay 1"
        input "gpioNumber2", "text", title: "GPIO#", required: false, defaultValue: "21"
        input "deviceTimeStart1", "text", title: "Time relay ON (from)", required: false, defaultValue: "08:00"
        input "deviceTimeEnd1", "text", title: "Time relay ON (to)", required: false, defaultValue: "01:00"
    }
    */
}

def installed() {
    //log.debug "Installed with settings: ${settings}"

    initialize()
}

def initialize() {

    subscribe(location, null, response, [filterEvents: false])

    setupVirtualRelay((String)deviceName1, (String)gpioNumber1, (String)deviceTimeStart1, (String)deviceTimeEnd1)
    setupVirtualRelay((String)deviceName2, (String)gpioNumber2, (String)deviceTimeStart2, (String)deviceTimeEnd2)
}

def updated() {
    //log.debug "Updated with settings: ${settings}"

    updateGPIOState()
    unsubscribe()

    updateVirtualRelay((String)deviceName1, (String)gpioNumber1, (String)deviceTimeStart1, (String)deviceTimeEnd1)
    updateVirtualRelay((String)deviceName2, (String)gpioNumber2, (String)deviceTimeStart2, (String)deviceTimeEnd2)

    subscribe(location, null, response, [filterEvents: false])
}

def updateVirtualRelay(String deviceName, String gpioNumber, String relayOnTimeStart, String relayOnTimeEnd) {

    // If user didn't fill this device out, skip it
    if (!deviceName) return

    String theDeviceNetworkId = getRelayID((String)settings.piIP, gpioNumber)

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
            setupVirtualRelay(deviceName, gpioNumber, relayOnTimeStart, relayOnTimeEnd)
        }
    }

}

def setupVirtualRelay(String deviceName, String gpioNumber, String deviceTimeStart, String deviceTimeEnd) {

    if (deviceName) {
        /*
        log.debug deviceName
        log.debug gpioNumber
        log.debug deviceTimeStart
        log.debug deviceTimeEnd
        */

        //log.trace "Found a relay switch called $deviceName on GPIO #${gpioNumber}"
        def d = addChildDevice("ibeech", "Virtual Pi Relay", getRelayID((String)settings.piIP, gpioNumber), theHub.id, [label: deviceName, name: deviceName])
        subscribe(d, "switch", switchChange)

        /*
        log.debug "Setting initial state of $gpioName to off"
        setDeviceState(gpioNumber, "off")
        d.off()
        */

    }
}

static String getRelayID(String piIP, String gpioNumber) {
    return "piRelay." + piIP + "." + gpioNumber
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

def updateGPIOState() {

    //log.trace "Updating GPIO map"

    executeRequest("/*", "GET", false, null)

    runIn(10, updateGPIOState)
}

def switchChange(evt) {

    //log.debug "switchChange: switch event, value: ${evt.value}"

    if (evt.value == "on" || evt.value == "off") return

    String[] parts = evt.value.tokenize('.')
    //String deviceId = parts[1]
    String GPIO = parts[5]
    String state = parts[6]

    //log.debug "switchChange: state: ${state}"

    switch (state) {
        case "refresh":
            // Refresh this switches button
            //log.debug "Refreshing the state of GPIO " + GPIO
            executeRequest("/*", "GET", false, null)
            return
    }

    setDeviceState(GPIO, state, (String)deviceTimeStart1, (String)deviceTimeEnd1)
}

def setDeviceState(String gpio, String state, String timeStart, String timeEnd) {
    //log.debug "Executing 'setDeviceState(${gpio}, '${state}', '${timeStart}', '${timeEnd}')"

    // Determine the path to post which will set the switch to the desired state
    String Path = "/GPIO/" + gpio + "/value/"
    Path += (state == "on") ? "1" : "0"

    executeRequest(Path, "POST", true, gpio)
}

def executeRequest(String Path, String method, boolean setGPIODirection, String gpioPin) {

    //log.debug "The " + method + " path is: " + Path

    LinkedHashMap headers = [:]
    headers.put("HOST", "$settings.piIP:$settings.piPort")

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

/* Helper functions to get the network device ID */

private String NetworkDeviceId() {
    String iphex = convertIPtoHex(settings.piIP).toUpperCase()
    String porthex = convertPortToHex(settings.piPort)
    return iphex + porthex
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize('.').collect { String.format('%02x', it.toInteger()) }.join()
    //log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private static String convertPortToHex(port) {
    String hexport = port.toString().format('%04x', port.toInteger())
    //log.debug hexport
    return hexport
}
