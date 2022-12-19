# Dev Server
A combined hub and map server for local development and single server execution.

## Local Development
A docker-compose file is provided for local development. It can be started one of two ways:
- `docker-compose up -d` executed in `bin/development/local`.
- Press the green play button in `bin/development/local/docker-compose.yml` in IntelliJ.

The compose manifest provides the following services:
- MongoDB
- MinIO

From there, the development server can be started with `./gradlew run :bin:development:run`, or the green
play button in the main class.

### Temp Note
Currently, the dev server requires a minio connection, though Mongo is optional. First make sure the compose file
is running as outlined above, and then create a mapmaker bucket in minio by following these steps:
- Navigate to `http://localhost:9001` in your browser.
- Sign in using the credentials `mapmaker` and `mapmaker`.
- Click `Create Bucket` in the top right.
- Name the bucket `mapmaker` and leave all other settings default.

Finally, start the dev server using the `DevServer (mongo)` run configuration provided in the repository (under `.run`).
