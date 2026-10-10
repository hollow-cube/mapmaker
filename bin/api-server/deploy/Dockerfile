# debian 13 for glibc 2.38 required for G1 native image.
# cc for onnxruntime which links libstdc++ and libgcc_s dynamically.
FROM gcr.io/distroless/cc-debian13

USER 65532:65532

WORKDIR /app
COPY ./bin/api-server/build/image/chat-filter/ /app/chat-filter/
COPY ./bin/api-server/build/image/lib/ /app/lib/
COPY ./bin/api-server/build/native/nativeCompile/api-server /app/api-server

EXPOSE 9124

ENTRYPOINT ["/app/api-server", "-Xmx384m", "-Dlogback.configurationFile=logback-prod.xml"]
