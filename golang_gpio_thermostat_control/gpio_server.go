package main

import (
	"encoding/json"
	"fmt"
	"github.com/gorilla/mux"
	"github.com/stianeikeland/go-rpio"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strconv"
	"bytes"
)

type GpioPort struct {
	Number    string `json:"number"`
	State     string `json:"state"`
	TimeStart string `json:"timestart"`
	TimeEnd   string `json:"timeend"`
}

var gpioPorts []GpioPort

func GetGpioEndpoint(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	params := mux.Vars(r)
	portNumberIn, _ := params["number"]
	for _, item := range gpioPorts {
		if item.Number == portNumberIn {
			json.NewEncoder(w).Encode(item)
			return
		}
	}
	http.Error(w, "Cannot find requested GPIO", http.StatusBadRequest)
}

func GetAllGpioEndpoint(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	json.NewEncoder(w).Encode(gpioPorts)
}

func compareGpio(a, b GpioPort) bool {
	if &a == &b {
		return true
	}
	if a.Number != b.Number {
		return false
	}
	/*
		if a.State != b.State {
			return false
		}
	*/
	if a.TimeStart != b.TimeStart {
		return false
	}
	if a.TimeEnd != b.TimeEnd {
		return false
	}
	return true
}

func ModifyGpioEndpoint(w http.ResponseWriter, r *http.Request) {

	// debug output
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Printf("Error reading body: %v", err)
		http.Error(w, "can't read body", http.StatusBadRequest)
		return
	}
	log.Print(string(body[:]))
	// end debug output

	params := mux.Vars(r)
	portNumberInt, err := strconv.Atoi(params["number"])
	if err != nil {
		http.Error(w, fmt.Sprintf("Incorrect GPIO number passed: %s", params["number"]), http.StatusBadRequest)
	}

	var gpioPort GpioPort
	//_ = json.NewDecoder(r.Body).Decode(&gpioPort)
	_ = json.NewDecoder(bytes.NewReader(body)).Decode(&gpioPort)
	gpioPort.Number = params["number"]

	if (gpioPort.State != "0") && (gpioPort.State != "1") {
		http.Error(w, fmt.Sprintf("Incorrect GPIO state passed: %s", gpioPort.State), http.StatusBadRequest)
	}

	found := false
	for index, item := range gpioPorts {
		if item.Number == params["number"] {
			found = true
			fmt.Printf("Found, comparing\n %+v\n %+v\n", gpioPorts[index], gpioPort)
			if !compareGpio(gpioPorts[index], gpioPort) {
				//fmt.Print("Not equal, saving config")
				gpioPorts[index] = gpioPort
				saveConfig()
			} /* else {
				//fmt.Print("Equal, not saving anything")
			}
			*/
			break
		}
	}

	if !found {
		gpioPorts = append(gpioPorts, gpioPort)
		saveConfig()
	}

	//w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	//json.NewEncoder(w).Encode(gpioPorts)

	if err := rpio.Open(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	defer rpio.Close()

	pin := rpio.Pin(portNumberInt)

	pin.Output()

	if gpioPort.State == "1" {
		pin.High()
	} else if gpioPort.State == "0" {
		pin.Low()
	}
}

func saveConfig() {
	// save config to disk
	fmt.Print("Saving config to disk")
	b, err := json.MarshalIndent(gpioPorts, "", "  ")
	if err != nil {
		panic(err)
	}
	os.Stdout.Write(append(b, '\n'))
}

func main() {
	router := mux.NewRouter()
	gpioPorts = append(gpioPorts, GpioPort{Number: "14", State: "1", TimeStart: "08:00", TimeEnd: "01:00"})
	router.HandleFunc("/GPIO", GetAllGpioEndpoint).Methods("GET")
	router.HandleFunc("/GPIO/{number}", GetGpioEndpoint).Methods("GET")
	router.HandleFunc("/GPIO/{number}", ModifyGpioEndpoint).Methods("POST")
	log.Fatal(http.ListenAndServe(":8001", router))
}
