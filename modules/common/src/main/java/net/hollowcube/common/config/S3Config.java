package net.hollowcube.common.config;

import org.jetbrains.annotations.NotNull;

public interface S3Config {
    @NotNull String address();
    @NotNull String region();
    @NotNull String accessKey();
    @NotNull String secretKey();
}
