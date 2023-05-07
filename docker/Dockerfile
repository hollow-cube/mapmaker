FROM eclipse-temurin:17-jre-ubi9-minimal as runtime

WORKDIR /app
COPY ./bin/development/build/libs/development-all.jar /app/server.jar

EXPOSE 25565
CMD ["java", "--enable-preview", "--add-modules", "jdk.incubator.concurrent", "-jar", "server.jar"]
