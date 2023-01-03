package net.hollowcube.mapmaker.metrics;

public class Metric {

    private int id;
    private String source;
    private String target;
    private double value;

    public Metric(int id, String source, String target, double value) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.value = value;
    }

    public Metric() { }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return this.source;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return this.target;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return this.value;
    }
}
