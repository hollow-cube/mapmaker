# Development Setup

This document is a heavy work in progress.

We currently maintain two local development setups which can be useful for different purposes:

* Tilt - Full local cluster similar to production
* Docker Compose - Simplified environment without service rebuild or multi-server support

## Tilt

TODO: currently lives in hollow-cube/development and only available to hc devs. Will be made public in the future.

## Docker Compose

To start, run the dependencies and api-server via docker compose:

```shell
git clone git@github.com:hollow-cube/api-server.git api-server
git clone git@github.com:hollow-cube/mapmaker.git mapmaker --recursive
# Its important that api-server is a sibling of mapmaker, and that mapmaker is cloned recursively.

cd mapmaker
./docker-compose-setup.sh
docker compose up -d

# Set some useful defaults
cat <<EOF > .env
MAPMAKER_UNLEASH_DEFAULT_ACTION=true
MAPMAKER_TRACING_NOOP=true
MAPMAKER_GLOBAL_NOOP=false
MAPMAKER_RESOURCE_PACK_HASH=dev
EOF
```

Next you can run the `DevServer` task in the mapmaker IntelliJ project.
The entrypoint is `bin/development/src/main/java/net/hollowcube/mapmaker/dev/DevMain.java`.

This will run a local merged hub/map server, but you can easily debug and profile it locally.

## FAQ

##### Loading the resource pack

The resource pack is not sent by the server automatically currently (to avoid reloading it on every restart).

The assets are packaged on every `DevServer` run, the jar can be explicitly built using the `Build RP` task
(or `./gradlew :buildClient`).

You can install it in two ways:

* (recommended) Symlink `bin/packer/build/packer-out/client` to your resource pack folder. Note that in recent versions
  you must also create an `allowed_symlinks.txt` file in the game directory with the symlink path.
* Run `Build RP` and copy `build/client.zip` to your resource pack folder.

##### Elevated permissions

You can give your account extra permissions by editing your player_data entry in the database and relogging.

```
psql "postgresql://postgres:postgres@localhost:5432/postgres"
> UPDATE player_data SET role = 'dev_3' WHERE username = 'notmattw';
```
