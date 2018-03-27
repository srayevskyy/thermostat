/**
 *  Pi Relay Control
 *
 *  Copyright 2016 Tom Beech
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

preferences {
    input("ip", "string", title: "IP Address", description: "192.168.86.226", defaultValue: "192.168.86.226", required: true, displayDuringSetup: true)
    input("port", "string", title: "Port", description: "8001", defaultValue: "8001", required: true, displayDuringSetup: true)
    input("gpioPort", "string", title: "GPIO Port", description: "21", defaultValue: "21", required: true, displayDuringSetup: true)
}

metadata {
    definition(name: "Virtual Pi Relay", namespace: "ibeech", author: "ibeech") {
        capability "Switch"
        capability "Refresh"
        capability "Polling"

        attribute 'timeStart', 'string'
        attribute 'timeEnd', 'string'

        command "changeSwitchState", ["string"]
        command "poll"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles (scale: 2) {

        standardTile("switch", "device.switch", width: 3, height: 3, canChangeIcon: true) {
            state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
            state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
        }

        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 3) {
            state("default", label: 'refresh', action: "polling.poll", icon: "st.secondary.refresh-icon")
        }

        valueTile("timeStartLabel", "device.label.timeStart", width: 2, height: 2) {
            state "default", label: 'Time Start'
        }

        valueTile("timeStart", "device.timeStart", width: 3, height: 2) {
            state "default", label: '${currentValue}'
        }

        valueTile("timeEndLabel", "device.label.timeEnd", width: 2, height: 2) {
            state "default", label: 'Time End'
        }

        valueTile("timeEnd", "device.timeEnd", decoration: "flat", width: 3, height: 2) {
            state "default", label:'${currentValue}'
        }

        main('switch')
        details(['switch', 'refresh', 'timeStartLabel', 'timeStart', 'timeEndLabel', 'timeEnd'])

    }
}

// parse events into attributes
def parse(description) {
    //log.debug "Virtual siwtch parsing '${description}'"

    //def evt1 = createEvent(name: "timeStart", value: '')
    //def evt2 = createEvent(name: "timeEnd", value: '')
    
    //return [evt1, evt2]

    log.debug "Parsing '${description?.json}'"
    def msg = parseLanMessage(description?.body)
    log.debug "Msg: '${msg}'"
    def json = parseJson(description?.body)
    log.debug "JSON: '${json}'"

    if (json.containsKey("timestart")) {
        sendEvent(name: "timeStart", value: json.timestart)
    }
    if (json.containsKey("timeend")) {
        sendEvent(name: "timeEnd", value: json.timeend)
    }
}

// handle commands
def poll() {
    log.debug "Executing 'poll'"
    getPiInfo()
}

def refresh() {
    log.debug "Executing 'refresh'"
    getPiInfo()
}

/*
def poll() {
    log.debug "Executing 'poll'"

    def timeStartValue = device.currentValue('timeStart')

    log.debug "device.currentValue(timeStart): " + device.currentValue("timeStart")
    log.debug "device.currentValue(timeEnd): " + device.currentValue("timeEnd")

    if (timeStartValue) {
        log.debug "From device handler: Executing 'ON', timeStart: " + timeStartValue.value
    } else {
        log.debug "From device handler: crap, timeStartValue is null"
    }

    def lastState = device.currentValue("switch")
    sendEvent(name: "switch", value: device.deviceNetworkId + ".refresh")
    sendEvent(name: "switch", value: lastState)
}

def refresh() {
    poll()
}
*/

def on() {
    log.debug "Executing 'on'"

    //sendEvent(name: "switch", value: device.deviceNetworkId + ".on")
    //sendEvent(name: "switch", value: "on")
}

def off() {
    log.debug "Executing 'off'"

    //sendEvent(name: "switch", value: device.deviceNetworkId + ".off")
    //sendEvent(name: "switch", value: "off")
}

/*
def changeSwitchState(newState) {
    log.trace "Received update that this switch is now $newState"
    switch (newState) {
        case 1:
            sendEvent(name: "switch", value: "on")
            break
        case 0:
            sendEvent(name: "switch", value: "off")
            break
    }
}
*/

private String convertIPtoHex(ipAddress) {
    log.debug "convertIPtoHex ${ipAddress} to hex"
    String hex = ipAddress.tokenize('.').collect { String.format('%02x', it.toInteger()) }.join()
    return hex
}

private String convertPortToHex(port) {
    log.debug "convertPortToHex ${port} to hex"
    String hexport = port.toString().format('%04x', port.toInteger())
    return hexport
}

private getPiInfo() {
    def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)

    def uri = "/GPIO/${gpioPort}"

    log.debug "URI: ${uri}"

    def headers = [:]
    headers.put("HOST", "${ip}:${port}")
    headers.put("Accept", "application/json")
    def hubAction = new physicalgraph.device.HubAction(
            method: "GET",
            path: uri,
            headers: headers,
            "${iphex}:${porthex}",
            [callback: parse]
    )
    log.debug "Getting Pi data ${hubAction}"
    hubAction
}

def sync(ip, port) {
    log.debug "sync ${ip} ${port}"
    def existingIp = getDataValue("ip")
    def existingPort = getDataValue("port")
    if (ip && ip != existingIp) {
        updateDataValue("ip", ip)
    }
    if (port && port != existingPort) {
        updateDataValue("port", port)
    }
    def ipHex = convertIPToHex(ip)
    def portHex = convertPortToHex(port)
    device.deviceNetworkId = "${ipHex}:${portHex}"
}

