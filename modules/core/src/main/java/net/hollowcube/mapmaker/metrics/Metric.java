package net.hollowcube.mapmaker.metrics;

import java.util.ArrayList;
import java.util.Collections;

public class Metric<T> {

    private int tag;
    private long timestamp;
    private ArrayList values = new ArrayList();

    public Metric(int tag, long timestamp, ArrayList values) {
        this.tag = tag;
        this.timestamp = timestamp;
        this.values = values;
    }

    public Metric(int tag, long timestamp, T ... values) {
        this.tag = tag;
        this.timestamp = timestamp;
        Collections.addAll(this.values, values);
    }

    public Metric(int tag, long timestamp) {
        this.tag = tag;
        this.timestamp = timestamp;
    }

    public Metric(int tag) {
        this.tag = tag;
    }

    public Metric() { }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return this.tag;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setValues(ArrayList values) {
        this.values = values;
    }

    public void setValue(int index, T value) {
        this.values.set(index, value);
    }

    public ArrayList getValues() {
        return this.values;
    }

    public String asString() {
        String metricPrintOut = "[" + timestamp + " " + tag + "] ";
        for (Object value : values) {
            metricPrintOut += value.toString() + ", ";
        }
        return metricPrintOut.substring(0, metricPrintOut.length() - 2);
    }
}
