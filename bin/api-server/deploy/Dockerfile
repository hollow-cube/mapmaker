FROM gcr.io/distroless/base-debian12

USER 65532:65532

WORKDIR /app
COPY ./bin/api-server/build/native/nativeCompile/api-server /app/api-server

EXPOSE 9124
ENTRYPOINT ["/app/api-server"]
