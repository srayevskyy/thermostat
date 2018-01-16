package main

import (
	"github.com/gorilla/mux"
	"log"
	"net/http"
	"os"
)

const local_dir = "/mnt/usb"
const url_path_prefix = "/video/"

func main() {
	router := mux.NewRouter()

	// check if directory exists
	if _, err := os.Stat(local_dir); err != nil {
		log.Fatalf("FATAL: Problem accessing directory '%s', cannot continue", local_dir)
	}

	router.PathPrefix(url_path_prefix).Handler(http.StripPrefix(url_path_prefix, http.FileServer(http.Dir(local_dir))))
	log.Fatal(http.ListenAndServe(":8001", router))
}
