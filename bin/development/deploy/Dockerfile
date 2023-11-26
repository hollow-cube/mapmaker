FROM eclipse-temurin:20-jre-ubi9-minimal

WORKDIR /app
COPY ./development-all.jar /app/server.jar

EXPOSE 25565
CMD ["java", "--enable-preview", "--add-modules", "jdk.incubator.concurrent", "-Xmx3G", "-jar", "server.jar"]
