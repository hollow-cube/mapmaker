FROM eclipse-temurin:24-alpine

WORKDIR /app
COPY ./bin/map/build/libs/map-all.jar /app/server.jar

EXPOSE 25565
CMD ["java", "-XX:+UseG1GC", "-Xms3000M", "-Xmx3000M", "-XX:ActiveProcessorCount=4", "-XX:NativeMemoryTracking=summary", "-XX:MaxDirectMemorySize=512m", "-jar", "server.jar"]
