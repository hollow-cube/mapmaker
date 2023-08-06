package net.hollowcube.map.feature.mapsize;

public enum MapSizeData {


    STANDARD(50),
    LARGE(100),
    SETH(1000000) // Basically unrestricted, also it's named seth cause I can't think of a better name
    ;

    private final double radius;

    MapSizeData(double radius) {
        this.radius = radius;
    }

    public double radius() {
        return radius;
    }
}
