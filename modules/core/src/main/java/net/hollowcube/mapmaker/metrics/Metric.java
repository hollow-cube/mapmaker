package net.hollowcube.mapmaker.metrics;

import java.io.Serializable;
import java.util.List;

public class Metric implements Serializable {
    private final MetricType metricType;
    private final List<Object> values;

    public Metric(MetricType metricType, List<Object> values) {
        if (metricType.getExpectedTypes().size() != values.size()) {
            throw new IllegalArgumentException("Invalid number of values for this metric type.");
        }

        for (int i = 0; i < metricType.getExpectedTypes().size(); i++) {
            Class<?> expectedType = metricType.getExpectedTypes().get(i);
            Object value = values.get(i);

            if (!expectedType.isInstance(value)) {
                throw new IllegalArgumentException(
                        "Invalid type for metric. Expected: " + expectedType + ", Received: " + value.getClass());
            }
        }

        this.metricType = metricType;
        this.values = values;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public List<Object> getValues() {
        return values;
    }
}
