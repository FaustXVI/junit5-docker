package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
)

func main() {
	go startHTTPOn("/hello", "8080", helloHandler)
	startHTTPOn("/env", "8081", envHandler)
}

func startHTTPOn(path string, port string, handler http.HandlerFunc) {
	http.HandleFunc(path, handler)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}

func helloHandler(writer http.ResponseWriter, request *http.Request) {
	fmt.Fprintf(writer, "Hello world\n")
}
func envHandler(writer http.ResponseWriter, request *http.Request) {
	envs := os.Environ()
	for _, env := range envs {
		fmt.Fprintf(writer, "%s\n", env)
	}
}
