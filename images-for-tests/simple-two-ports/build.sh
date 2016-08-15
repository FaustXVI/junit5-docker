#!/bin/sh
set -e

docker run -v $(pwd):/go \
    -e CGO_ENABLED=0 \
    golang:1.6.3-alpine \
    go build -a --installsuffix cgo --ldflags="-s" -o ./hello

docker build -t faustxvi/simple-two-ports .
docker push faustxvi/simple-two-ports

docker build -t faustxvi/simple-two-ports:wrong-one - <<EOF
FROM scratch
LABEL is=wrong
EOF

docker push faustxvi/simple-two-ports:wrong-one
