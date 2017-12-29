package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/goiot/devices/monochromeoled"
	"github.com/gorilla/mux"
	"github.com/stianeikeland/go-rpio"
	"golang.org/x/exp/io/i2c"
	"golang.org/x/image/font"
	"golang.org/x/image/font/basicfont"
	"golang.org/x/image/math/fixed"
	"image"
	"image/color"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"
)

type GpioPort struct {
	Number     string `json:"number"`
	State      string `json:"state"`
	TimeStart  string `json:"timestart"`
	TimeEnd    string `json:"timeend"`
	DisplayDev string `json:"displaydev"`
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

	setGpioState(portNumberInt, gpioPort.State)

}

func setGpioState(portNumber int, portState string) {
	if err := rpio.Open(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	defer rpio.Close()

	pin := rpio.Pin(portNumber)

	pin.Output()

	if portState == "1" {
		pin.High()
	} else if portState == "0" {
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

func addLabel(img *image.RGBA, x, y int, label string) {
	col := color.RGBA{200, 100, 0, 255}
	point := fixed.Point26_6{fixed.Int26_6(x * 64), fixed.Int26_6(y * 64)}

	d := &font.Drawer{
		Dst:  img,
		Src:  image.NewUniform(col),
		Face: basicfont.Face7x13, /*inconsolata.Regular8x16*/
		Dot:  point,
	}
	d.DrawString(label)
}

func displayData() {
	for {
		for _, gpioPort := range gpioPorts {
			if gpioPort.DisplayDev != "" {
				img := image.NewRGBA(image.Rect(0, 0, 128, 64))
				addLabel(img, 0, 20, time.Now().Format("15:04:05")+" PORT: "+gpioPort.Number)
				stateString := "ON"
				if gpioPort.State == "0" {
					stateString = "OFF"
				}
				addLabel(img, 0, 40, "State: "+stateString)
				addLabel(img, 0, 60, gpioPort.TimeStart+" - "+gpioPort.TimeEnd)

				d, err := monochromeoled.Open(&i2c.Devfs{Dev: gpioPort.DisplayDev})
				if err != nil {
					panic(err)
				}

				if err := d.SetImage(0, 0, img); err != nil {
					panic(err)
				}

				if err := d.Draw(); err != nil {
					panic(err)
				}
				d.Close()
			}
		}
		time.Sleep(5 * time.Second)
	}
}

func main() {
	router := mux.NewRouter()
	gpioPorts = append(gpioPorts, GpioPort{Number: "21", State: "0", TimeStart: "08:00", TimeEnd: "01:00", DisplayDev: "/dev/i2c-1"})
	setGpioState(21, "0")
	router.HandleFunc("/GPIO", GetAllGpioEndpoint).Methods("GET")
	router.HandleFunc("/GPIO/{number}", GetGpioEndpoint).Methods("GET")
	router.HandleFunc("/GPIO/{number}", ModifyGpioEndpoint).Methods("POST")
	go displayData()
	log.Fatal(http.ListenAndServe(":8001", router))
}
