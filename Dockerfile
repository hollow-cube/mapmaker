FROM ubuntu:latest
LABEL authors="matt"

ENTRYPOINT ["top", "-b"]