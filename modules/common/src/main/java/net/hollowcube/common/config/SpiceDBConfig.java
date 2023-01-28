package net.hollowcube.common.config;

import org.jetbrains.annotations.NotNull;

public interface SpiceDBConfig {

    @NotNull String address();

    @NotNull String secretKey();

    boolean tls();

}
