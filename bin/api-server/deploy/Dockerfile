# debian13, not 12: --static-nolibc leaves libc dynamic, and the G1 binary the build image
# produces needs GLIBC_2.38, which is newer than bookworm's 2.36. Serial-GC images (map-isolate)
# stay under that line, which is why they are fine on 12.
FROM gcr.io/distroless/base-debian13

USER 65532:65532

WORKDIR /app
COPY ./bin/api-server/build/native/nativeCompile/api-server /app/api-server

EXPOSE 9124
# Structured logs in the cluster; logback.xml (plain text) is what a local run picks up.
ENTRYPOINT ["/app/api-server", "-Dlogback.configurationFile=logback-prod.xml"]
