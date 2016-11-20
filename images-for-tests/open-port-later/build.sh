#!/bin/sh
set -e

docker run -v $(pwd):/go \
    -e CGO_ENABLED=0 \
    golang:1.6.3-alpine \
    go build -a --installsuffix cgo --ldflags="-s" -o ./hello

docker build -t faustxvi/open-port-later .
docker push faustxvi/open-port-later

