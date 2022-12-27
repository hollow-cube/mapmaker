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

    public void addMetricFirstJoinTime(String uuid, double timestamp_ms) {
        Metric metric = new Metric(MetricsEnum.USER_FIRST_JOIN_TIME_MS.ordinal(), uuid, "", timestamp_ms);
        metricManager.addMetric(metric);
    }

    public void addMetricJumpCount(String uuid) {
        int id = MetricsEnum.USER_JUMP_COUNT.ordinal();
        Double jumps;
        if ((jumps = metricManager.getValue(id, uuid, "")) != null) { }
        else {
            System.out.println("Could not find existing jump count for player " + uuid);
        }
        Metric metric = new Metric(id, uuid, "", jumps + 1);
        metricManager.addMetric(metric);
    }

    public void addMetricPlayTimeMs(String uuid, double timestamp_ms) {
        Metric metric = new Metric(MetricsEnum.USER_PLAY_TIME_MS.ordinal(), uuid, "", timestamp_ms);
        metricManager.addMetric(metric);
    }
}