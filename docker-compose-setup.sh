# Assumes you have map-service, session-service, and player-service in `..`

set -e
ORIGINAL_DIR=$(pwd)

docker image rm --force mapmaker-api-server:latest && true

export GOOS=linux
export GOARCH=amd64
if [[ "$(uname -m)" == "aarch64" || "$(uname -m)" == "arm64" ]]; then
  export GOARCH=arm64
fi
export CGO_ENABLED=0

cd ../hc-services
go build -o api-server cmd/api-server/*.go && true
cd ${ORIGINAL_DIR}
