package main

import (
	"fmt"
	"log"
	"net/http"
)

func main() {
	startHTTPOn("/env", "8080", helloHandler)
}

func startHTTPOn(path string, port string, handler http.HandlerFunc) {
	http.HandleFunc(path, handler)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}

func helloHandler(writer http.ResponseWriter, request *http.Request) {
	fmt.Fprintf(writer, "Hello world\n")
}
