# Assumes you have api-server in `..`

set -e
ORIGINAL_DIR=$(pwd)

docker image rm --force mapmaker-api-server:latest && true

export GOOS=linux
export GOARCH=amd64
if [[ "$(uname -m)" == "aarch64" || "$(uname -m)" == "arm64" ]]; then
  export GOARCH=arm64
fi
export CGO_ENABLED=0

cd ../api-server
go build -o api-server cmd/api-server/*.go && true
cd ${ORIGINAL_DIR}

# upload the hub map world so we can load it in devserver
HUB_MAP_ID=b210fa07-d64c-4100-a2db-1426c35b7533
HUB_WORLD=bin/development/hcspawn.polar

docker compose up -d minio
mc() {
  docker compose exec -T -e MC_HOST_local=http://mapmaker:mapmaker@localhost:9000 minio mc "$@"
}
until mc ready local >/dev/null 2>&1; do sleep 1; done
mc mb --ignore-existing local/mapmaker

local_md5=$(openssl md5 -r "${HUB_WORLD}" | cut -d' ' -f1)
remote_md5=$(mc stat --json "local/mapmaker/${HUB_MAP_ID}" 2>/dev/null | sed -n 's/.*"etag":"\([0-9a-f]*\)".*/\1/p')
if [[ "${local_md5}" != "${remote_md5}" ]]; then
  docker compose cp "${HUB_WORLD}" minio:/tmp/hcspawn.polar
  mc cp /tmp/hcspawn.polar "local/mapmaker/${HUB_MAP_ID}"
else
  echo "Hub world is up to date"
fi
