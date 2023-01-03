package net.hollowcube.mapmaker.metrics;

public final class MetricsHelper {

    private enum MetricsEnum {
        USER_TOTAL_PLAY_TIME_MS("USER_TOTAL_PLAY_TIME_MS"),
        USER_SESSION_PLAY_TIME_MS("USER_SESSION_PLAY_TIME_MS"),
        USER_JUMP_COUNT("USER_JUMP_COUNT"),
        USER_FIRST_JOIN_TIME_MS("USER_FIRST_JOIN_TIME_MS"),
        ;

        private final String text;

        MetricsEnum(String text) {
            this.text = text;
        }
    };

    static private MetricManager metricManager;

    private MetricsHelper(MetricManager metricManager) {
        this.metricManager = metricManager;
    }

    public static String getMetricString(int id) {
        return MetricsEnum.values()[id].text;
    }

    public static int getMetricId(String name) {
        return MetricsEnum.valueOf(name).ordinal();
    }

    /**
     * Records a new metric for player's first join time
     * @param uuid Player UUID
     * @param timestamp_ms Time since epoch in milliseconds
     */
    public static void recordMetricFirstJoinTime(String uuid, double timestamp_ms) {
        Metric metric = new Metric(MetricsEnum.USER_FIRST_JOIN_TIME_MS.ordinal(), uuid, "", timestamp_ms);
        metricManager.addMetric(metric);
    }

    /**
     * Updates player's jump count metric by one
     * @param uuid Player UUID
     */
    public static void recordMetricJumpCount(String uuid) {
        int id = MetricsEnum.USER_JUMP_COUNT.ordinal();
        Double jumps;
        if ((jumps = metricManager.getValue(id, uuid, "")) != null) { }
        else {
            System.out.println("Could not find existing jump count for player " + uuid);
        }
        Metric metric = new Metric(id, uuid, "", jumps + 1);
        metricManager.addMetric(metric);
    }

    /**
     * Records a new metric for player's total play time
     * @param uuid Player UUID
     * @param timestamp_ms Time since epoch in milliseconds
     */
    public static void recordMetricTotalPlayTimeMs(String uuid, double timestamp_ms) {
        Metric metric = new Metric(MetricsEnum.USER_TOTAL_PLAY_TIME_MS.ordinal(), uuid, "", timestamp_ms);
        metricManager.addMetric(metric);
    }

    /**
     * Records a new metric for player's session play time
     * @param uuid Player UUID
     * @param timestamp_ms Time since epoch in milliseconds
     */
    public static void recordMetricSessionPlayTimeMs(String uuid, double timestamp_ms) {
        Metric metric = new Metric(MetricsEnum.USER_SESSION_PLAY_TIME_MS.ordinal(), uuid, "", timestamp_ms);
        metricManager.addMetric(metric);
    }
}