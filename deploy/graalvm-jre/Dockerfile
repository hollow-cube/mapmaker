##
# GraalVM 24 Java Runtime Environment for cloud-ready and deployment-friendly Docker images
# Author: Arthur Lemeux Martelet
##

# Set the target ubuntu version (can be overridden during build with --build-arg UBUNTU_VERSION=<version>)
ARG UBUNTU_VERSION=noble

## --------------------------------------------------------------
## 1. Build the GraalVM 24 JRE Image with jlink from the GraalVM JDK
## --------------------------------------------------------------
# Note: the --platform flag is used to tell buildx to use the HOST platform for multi-platform builds
#       and not using the TARGET platform when building the image. (e.g., --platform=linux/amd64 with buildx)
#       It means if you are using an ARM64 machine and want to build both ARM64 and AMD64 images,
#       it will use the ARM64 version of ubuntu:${UBUNTU_VERSION} with GraalVM JDK to build both AMD64 and ARM64 images with jlink to the next stage.
FROM --platform=$BUILDPLATFORM ubuntu:${UBUNTU_VERSION} AS graalvm-jre-builder

# a. Install binutils-multiarch package for jlink (multiarch support for both amd64 and arm64) and curl to download the GraalVM JDK tarball
RUN apt-get update && \
    apt-get install --no-install-recommends -y binutils-multiarch curl ca-certificates && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/*

# b. Create the /usr/local/lib/jvm directory
RUN mkdir -p /usr/local/lib/jvm

# c. Set the target java version (must be overridden during build with --build-arg JAVA_VERSION=<version>)
ARG JAVA_VERSION

# c.1 Download and extract the GraalVM 24 JDK tarball (amd64)
RUN curl -L "https://download.oracle.com/graalvm/24/archive/graalvm-jdk-${JAVA_VERSION}_linux-x64_bin.tar.gz" -o /tmp/graalvm-jdk-24-amd64.tar.gz && \
    curl -L "https://download.oracle.com/graalvm/24/archive/graalvm-jdk-${JAVA_VERSION}_linux-x64_bin.tar.gz.sha256" -o /tmp/graalvm-jdk-24-amd64.tar.gz.sha256 && \
    echo "$(cat /tmp/graalvm-jdk-24-amd64.tar.gz.sha256)  /tmp/graalvm-jdk-24-amd64.tar.gz" | sha256sum -c && \
    tar -xzf /tmp/graalvm-jdk-24-amd64.tar.gz -C /tmp && \
    rm /tmp/graalvm-jdk-24-amd64.tar.gz* && \
    mv /tmp/graalvm-jdk-* /usr/local/lib/jvm/graalvm-jdk-24-amd64
# c.2 Download and extract the GraalVM 24 JDK tarball (arm64)
RUN curl -L "https://download.oracle.com/graalvm/24/archive/graalvm-jdk-${JAVA_VERSION}_linux-aarch64_bin.tar.gz" -o /tmp/graalvm-jdk-24-arm64.tar.gz && \
    curl -L "https://download.oracle.com/graalvm/24/archive/graalvm-jdk-${JAVA_VERSION}_linux-aarch64_bin.tar.gz.sha256" -o /tmp/graalvm-jdk-24-arm64.tar.gz.sha256 && \
    echo "$(cat /tmp/graalvm-jdk-24-arm64.tar.gz.sha256)  /tmp/graalvm-jdk-24-arm64.tar.gz" | sha256sum -c && \
    tar -xzf /tmp/graalvm-jdk-24-arm64.tar.gz -C /tmp && \
    rm /tmp/graalvm-jdk-24-arm64.tar.gz* && \
    mv /tmp/graalvm-jdk-* /usr/local/lib/jvm/graalvm-jdk-24-arm64

# d. Get the BUILDARCH (automatically set by buildx)
ARG BUILDARCH

# e. Set the JAVA_HOME and PATH environment variables depending on the HOST platform
ENV JAVA_HOME=/usr/local/lib/jvm/graalvm-jdk-24-${BUILDARCH}
ENV PATH=$JAVA_HOME/bin:$PATH

# f. Copy the jmods.list file to get the list of modules to include in the custom JRE image
#    Note: The file is based on the Eclipse Temurin modules included in the JRE image.
#          I have onlu added the jdk.jcmd module to the list (for Flight Recorder support)
COPY jmods.list /tmp/jmods.list

# g. Get the TARGETARCH (automatically set by buildx)
ARG TARGETARCH

# h. Run jlink to create a custom JRE image of GraalVM 24 to the TARGET platform
RUN jlink \
    --add-modules "$(cat /tmp/jmods.list)" \
    --module-path "/usr/local/lib/jvm/graalvm-jdk-24-${TARGETARCH}/jmods" \
    --compress zip-6 \
    --no-header-files \
    --no-man-pages \
    --strip-debug \
    --output /graalvm-jre-24

## --------------------------------------------------------------
## 2. Build the final GraalVM 24 JRE Image
## --------------------------------------------------------------
# Note: As a final stage, we will use the ubuntu:${UBUNTU_VERSION} version with the TARGET platform to create the final image.
#       The stage will automatically use the right created JRE image from the previous stage.
FROM ubuntu:${UBUNTU_VERSION} AS graalvm-jre

# a. Update system and install ca-certificates and perform cleanup
RUN apt-get update && \
    apt-get install --no-install-recommends -y ca-certificates && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/*

# b. Copy the custom JRE image from the previous stage
COPY --from=graalvm-jre-builder /graalvm-jre-24 /usr/local/lib/jvm/graalvm-jre-24

# c. Set the JAVA_HOME and PATH environment variables
ENV JAVA_HOME=/usr/local/lib/jvm/graalvm-jre-24
ENV PATH=$JAVA_HOME/bin:$PATH

# d. Set the entrypoint to java
ENTRYPOINT ["java"]