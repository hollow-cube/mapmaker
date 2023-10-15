package net.hollowcube.mapmaker.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public enum MetricType {
    PLAYER_JOIN_SERVER(Arrays.asList(Long.class, UUID.class)),
    ;

    private final List<Class<?>> expectedTypes;

    MetricType(List<Class<?>> expectedTypes) {
        this.expectedTypes = expectedTypes;
    }

    public List<Class<?>> getExpectedTypes() {
        return expectedTypes;
    }
}
