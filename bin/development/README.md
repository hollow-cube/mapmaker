# Dev Server
A combined hub and map server for local development and single server execution.

## Local Development
A docker-compose file is provided for local development. It can be started one of two ways:
- `docker-compose up -d` executed in `bin/development/local`.
- Press the green play button in `bin/development/local/docker-compose.yml` in IntelliJ.

The compose manifest provides the following services:
- MongoDB

From there, the development server can be started with `./gradlew run :bin:development:run`, or the green
play button in the main class.
