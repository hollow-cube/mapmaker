FROM eclipse-temurin:17-jre-ubi9-minimal as runtime

WORKDIR /app
COPY ./server.jar /app/server.jar

EXPOSE 25565
CMD ["java", "-jar", "server.jar"]
