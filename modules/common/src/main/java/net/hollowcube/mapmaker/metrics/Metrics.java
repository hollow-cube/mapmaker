package net.hollowcube.mapmaker.metrics;

import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.NullString;

public enum MetricsEnum {
    USER_PLAY_TIME_MS,
    USER_JUMP_COUNT,
    USER_FIRST_JOIN_TIME_MS,
};

public class Metrics {

    private MetricManager metricManager;

    public Metrics(MetricManager metricManager) {
        this.metricManager = metricManager;
    }

    public boolean addMetricFirstJoinTime(String UUID, double timestamp_ms) {
        return metricManager.addMetric(MetricsEnum.USER_FIRST_JOIN_TIME_MS.ordinal(), UUID, "", timestamp_ms);
    }

    // TODO implement this to increment the locally tracked metric for this, since it will be extremely commonly updated.
    public boolean addMetricJumpCount(int UUID, double timestamp_ms) {
        return false;
    }
}