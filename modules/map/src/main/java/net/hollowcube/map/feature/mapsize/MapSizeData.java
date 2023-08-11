package net.hollowcube.map.feature.mapsize;

public enum MapSizeData {


    STANDARD(1000),
    LARGE(5000),
    SETH(29_999_984 /*Minecraft world limit*/) // Basically unrestricted, also it's named seth cause I can't think of a better name
    ;

    public final double radius;

    MapSizeData(double mapRadius) {
        this.radius = mapRadius;
    }
}
