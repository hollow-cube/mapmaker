FROM eclipse-temurin:21-jre-ubi9-minimal

WORKDIR /app

# Install async-profiler
ADD https://github.com/async-profiler/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz /app/async-profiler.tar.gz
RUN tar -xzf async-profiler.tar.gz && rm async-profiler.tar.gz && mv async-profiler-2.9-linux-x64 async-profiler
RUN chmod +x async-profiler/profiler.sh

COPY ./hub_deploy.jar /app/server.jar

EXPOSE 25565
CMD ["java", "--enable-preview", "-XX:+UseSerialGC", "-Xms500M", "-Xmx1500M", "-XX:ActiveProcessorCount=4", "-jar", "server.jar"]
