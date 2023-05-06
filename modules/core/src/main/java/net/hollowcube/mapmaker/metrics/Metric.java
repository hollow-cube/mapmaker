package net.hollowcube.mapmaker.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Metric<T> {
    /**
     * Supported value types
     */
    public enum ValueType {
        DOUBLE,
        STRING,
        BOOLEAN,
        INT32,
        INT64,
    }

    private String tag;
    private long timestamp;
    private final HashMap<Object, ValueType> values = new HashMap<>();

    public Metric(String tag, long timestamp, ArrayList values) {
        this.tag = tag;
        this.timestamp = timestamp;
        setValues(values);
    }

    public Metric(String tag, long timestamp, T... values) {
        this.tag = tag;
        this.timestamp = timestamp;
        setValues(new ArrayList(List.of(values)));
    }

    public Metric() {
    }

    public String getTag() {
        return this.tag;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setValues(ArrayList values) {
        for (var value : values) {
            if (value instanceof Integer) {
                this.values.put(value, ValueType.INT32);
            } else if (value instanceof Double) {
                this.values.put(value, ValueType.DOUBLE);
            } else if (value instanceof Long) {
                this.values.put(value, ValueType.INT64);
            } else if (value instanceof String) {
                this.values.put(value, ValueType.STRING);
            } else if (value instanceof Boolean) {
                this.values.put(value, ValueType.BOOLEAN);
            } else {
                System.out.println("Tried to pass unsupported type " +
                        value.getClass().toString() + " to Metric class, skipping.");
            }
        }
    }

    public HashMap<Object, ValueType> getValues() {
        return this.values;
    }

    public String asString() {
        String metricPrintOut = "[" + timestamp + " " + tag + "] ";
        for (Object o : values.entrySet()) {
            var hashValue = (Map.Entry) o;
            metricPrintOut += hashValue.getKey() + ", ";
        }
        return metricPrintOut.substring(0, metricPrintOut.length() - 2);
    }
}
