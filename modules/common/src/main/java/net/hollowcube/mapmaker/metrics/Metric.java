package net.hollowcube.mapmaker.metrics;

import java.util.ArrayList;

public class Metric<T> {

    private int tag;
    private long timestamp;
    private ArrayList values;

    public Metric(int tag, long timestamp, ArrayList values) {
        this.tag = tag;
        this.timestamp = timestamp;
        this.values = values;
    }

    public Metric(int tag, long timestamp, T ... values) {
        this.tag = tag;
        this.timestamp = timestamp;
        for (T value : values) {
            this.values.add(value);
        }
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
    
    public ArrayList getValues(int[] indices) {
        ArrayList values = null;
        for (int i : indices) {
            values.add(this.values.get(i));
        }
        return values;
    }

    public Object getValue(int index) {
        return this.values.get(index);
    }

    public boolean isUnique() {
        return MetricsHelper.isUnique(this.tag);
    }

    public int[] getMatchIndices() {
        return MetricsHelper.getMatchIndices(this.tag);
    }

    public int[] getUpdateIndices() {
        return MetricsHelper.getUpdateIndices(this.tag);
    }
}
