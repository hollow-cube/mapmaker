FROM mworzala/graalvm-jre:24

WORKDIR /app
COPY ./bin/map-isolate/build/libs/map-isolate-all.jar /app/server.jar

EXPOSE 25565
CMD ["-XX:+UseG1GC", "-Xms3000M", "-Xmx3000M", "-XX:ActiveProcessorCount=4", "-jar", "server.jar"]
