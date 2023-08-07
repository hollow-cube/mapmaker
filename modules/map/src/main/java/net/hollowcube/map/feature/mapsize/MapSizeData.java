package net.hollowcube.map.feature.mapsize;

public enum MapSizeData {


    STANDARD(50,10) {
        @Override
        public int getPrice() {
            //placeholder method
            return super.getPrice();
        }
    },
    LARGE(100,50),
    SETH(30_000_000,-1 /*Minecraft world limit*/) // Basically unrestricted, also it's named seth cause I can't think of a better name
    ;

    public final double xLimit,yLimit,zLimit;
    private final int price;

    MapSizeData(double radius,int price) {
        this(radius,radius,price);
    }

    MapSizeData(double radius, double yLimit, int price) {
        this(radius,yLimit,radius,price);
    }

    MapSizeData(double xLimit, double yLimit, double zLimit, int price) {
        this.xLimit = xLimit;
        this.yLimit = yLimit;
        this.zLimit = zLimit;

        this.price = price;
    }

    public int getPrice() {
        //implement custom logic if you will
        return price;
    }
}
