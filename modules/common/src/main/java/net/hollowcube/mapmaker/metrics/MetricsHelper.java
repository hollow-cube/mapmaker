package net.hollowcube.mapmaker.metrics;

public class MetricsHelper {

    private enum MetricsEnum {
        USER_PLAY_TIME_MS,
        USER_JUMP_COUNT,
        USER_FIRST_JOIN_TIME_MS,
    };

    static private MetricManager metricManager;

    public MetricsHelper(MetricManager metricManager) {
        this.metricManager = metricManager;
    }

    public boolean addMetricFirstJoinTime(String UUID, double timestamp_ms) {
        Metric metric = new Metric(MetricsEnum.USER_FIRST_JOIN_TIME_MS.ordinal(), UUID, "", timestamp_ms);
        return metricManager.addMetric(metric);
    }

    public boolean addMetricJumpCount(String UUID) {
        int id = MetricsEnum.USER_JUMP_COUNT.ordinal();
        Double jumps;
        if ((jumps = metricManager.getCachedValue(id, UUID, "")) != null) { }
        else if ((jumps = metricManager.getValue(id, UUID, "")) != null) { }
        else {
            System.out.println("Failed to update jump count for player " + UUID);
            return false;
        }
        return metricManager.updateMetricLocal(id, UUID, "", jumps + 1);
    }

    public boolean addMetricPlayTimeMs(String UUID, double timestamp_ms) {
        Metric metric = new Metric(MetricsEnum.USER_PLAY_TIME_MS.ordinal(), UUID, "", timestamp_ms);
        return metricManager.addMetric(metric);
    }
}