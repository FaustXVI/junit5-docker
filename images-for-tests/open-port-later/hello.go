package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"time"
)

const (
	waitEnv = "WAITING_TIME"
)

func main() {
	waitingTimeEnv := os.Getenv(waitEnv)
	waitingTime, err := time.ParseDuration(waitingTimeEnv)
	if err != nil {
		os.Exit(1)
	}
	time.Sleep(waitingTime)
	log.Print("started")
	startHTTPOn("/hello", "8080", helloHandler)
}

func startHTTPOn(path string, port string, handler http.HandlerFunc) {
	http.HandleFunc(path, handler)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}

func helloHandler(writer http.ResponseWriter, request *http.Request) {
	fmt.Fprintf(writer, "Hello world\n")
}
