preferences {
    input("ip", "string", title: "IP Address", description: "192.168.86.226", defaultValue: "192.168.86.226", required: true, displayDuringSetup: true)
    input("port", "string", title: "Port", description: "5002", defaultValue: "5002", required: true, displayDuringSetup: true)
}

metadata {
    definition(name: "Raspberry Pi Air quality monitor", namespace: "srayevskyy", author: "User") {
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        //capability "Health Check"

        attribute "co2", "number"
        attribute "tvoc", "number"
        attribute "temp", "number"
        attribute "lastTimeSensorRead", "number"

        command "poll"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {
        valueTile("co2Label", "device.label.co2", width: 1, height: 1) {
            state "default", label: 'co2'
        }
        valueTile("co2", "device.co2", width: 2, height: 1) {
            state "default", label: '${currentValue} PPM'
        }
        valueTile("tvocLabel", "device.label.tvoc", width: 1, height: 1) {
            state "default", label: 'tvoc'
        }
        valueTile("tvoc", "device.tvoc", width: 2, height: 1) {
            state "default", label: '${currentValue}\n'
        }
        valueTile("tempLabel", "device.label.temp", width: 1, height: 1) {
            state "default", label: 'temp'
        }
        valueTile("temp", "device.temp", width: 2, height: 1) {
            state "default", label: '${currentValue} C\n'
        }
        valueTile("lastTimeSensorReadLabel", "device.label.lastTimeSensorRead", width: 3, height: 1) {
            state "default", label: 'lastTimeSensorRead'
        }
        valueTile("lastTimeSensorRead", "device.lastTimeSensorRead", width: 2, height: 1) {
            state "default", label: '${currentValue}\n'
        }

        standardTile("refresh", "device.refresh", inactiveLabel: false, width: 1, height: 1, decoration: "flat") {
            state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        main "co2Label"

        details(["co2Label", "co2", "tvocLabel", "tvoc", "tempLabel", "temp", "lastTimeSensorReadLabel", "lastTimeSensorRead", "refresh"])
    }
}

def installed() {
    log.debug "installed"
    initialize();
}

def updated() {
    log.debug "updated"
    initialize();
}

def ping() {
    log.debug "ping"
    poll()
}

def initialize() {
    log.debug "initialize"
    sendEvent(name: "checkInterval", value: 60 * 10, data: [protocol: "cloud"], displayed: false)
    refresh()
}
// parse events into attributes
def parse(description) {
    log.debug "Parsing '${description?.json}'"
    def msg = parseLanMessage(description?.body)
    log.debug "Msg ${msg}"
    def json = parseJson(description?.body)
    log.debug "JSON '${json}'"

    if (json.containsKey("co2")) {
        sendEvent(name: "co2", value: json.co2)
    }
    if (json.containsKey("tvoc")) {
        sendEvent(name: "tvoc", value: json.tvoc)
    }
    if (json.containsKey("temp")) {
        sendEvent(name: "temp", value: json.temp)
    }
    if (json.containsKey("lastTimeSensorRead")) {
        sendEvent(name: "lastTimeSensorRead", value: json.lastTimeSensorRead)
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

private getPiInfo() {
    def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)

    def uri = "/sensor_values"
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

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex, 16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]), convertHexToInt(hex[2..3]), convertHexToInt(hex[4..5]), convertHexToInt(hex[6..7])].join(".")
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
