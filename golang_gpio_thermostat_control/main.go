package main

import (
	"encoding/json"
	"fmt"
	"github.com/gorilla/mux"
	"github.com/stianeikeland/go-rpio"
	"log"
	"net/http"
	"os"
	"strconv"
)

type GpioPort struct {
	Number    int    `json:"number`
	State     int    `json:"state"`
	TimeStart string `json:"timestart"`
	TimeEnd   string `json:"timeend"`
}

var gpioPorts []GpioPort

func GetGpioEndpoint(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	params := mux.Vars(r)
	portNumberIn, _ := strconv.Atoi(params["number"])
	for _, item := range gpioPorts {
		if item.Number == portNumberIn {
			json.NewEncoder(w).Encode(item)
			return
		}
	}
	json.NewEncoder(w).Encode(&GpioPort{})
}

func GetAllGpioEndpoint(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	json.NewEncoder(w).Encode(gpioPorts)
}

func ModifyGpioEndpoint(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	var gpioPort GpioPort
	_ = json.NewDecoder(r.Body).Decode(&gpioPort)
	portNumberIn, _ := strconv.Atoi(params["number"])
	gpioPort.Number = portNumberIn

	found := false
	for index, item := range gpioPorts {
		if item.Number == portNumberIn {
			found = true
			gpioPorts[index] = gpioPort
			break
		}
	}

	if !found {
		gpioPorts = append(gpioPorts, gpioPort)
	}

	//w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	//json.NewEncoder(w).Encode(gpioPorts)

	if err := rpio.Open(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	defer rpio.Close()

	pin := rpio.Pin(portNumberIn)

	pin.Output()

	if gpioPort.State == 1 {
		pin.High()
	} else {
		pin.Low()
	}
}

func main() {
	router := mux.NewRouter()
	gpioPorts = append(gpioPorts, GpioPort{Number: 21, State: 1, TimeStart: "08:00", TimeEnd: "01:00"})
	router.HandleFunc("/GPIO", GetAllGpioEndpoint).Methods("GET")
	router.HandleFunc("/GPIO/{number}", GetGpioEndpoint).Methods("GET")
	router.HandleFunc("/GPIO/{number}", ModifyGpioEndpoint).Methods("POST")
	log.Fatal(http.ListenAndServe(":8001", router))
}
