# Assumes you have map-service, session-service, and player-service in `..`

set -e
ORIGINAL_DIR=$(pwd)

docker image rm --force mapmaker-map-service:latest && true
docker image rm --force mapmaker-player-service:latest && true
docker image rm --force mapmaker-session-service:latest && true

export GOOS=linux
export GOARCH=amd64
export CGO_ENABLED=0

cd ../map-service && CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o map-service cmd/map-service/*.go && true
cd ../player-service && CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o player-service cmd/player-service/*.go && true
cd ../session-service && CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o session-service cmd/session-service/*.go && true
cd ${ORIGINAL_DIR}
