FROM eclipse-temurin:20-jre-ubi9-minimal

WORKDIR /app
COPY ./development-all.jar /app/server.jar

# Install async-profiler
ADD https://github.com/async-profiler/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz /app/async-profiler.tar.gz
RUN tar -xzf async-profiler.tar.gz && rm async-profiler.tar.gz && mv async-profiler-2.9-linux-x64 async-profiler
RUN chmod +x async-profiler/profiler.sh

EXPOSE 25565
CMD ["java", "--enable-preview", "--add-modules", "jdk.incubator.concurrent", "-Xmx3G", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints", "-jar", "server.jar"]
