FROM gcr.io/distroless/base

WORKDIR /app
COPY ./bin/map-isolate/build/native/nativeCompile /app

EXPOSE 25565
CMD ["./map-isolate"]
