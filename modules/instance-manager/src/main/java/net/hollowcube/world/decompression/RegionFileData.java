package net.hollowcube.world.decompression;

public class RegionFileData {
    //contents of the region file
    private final byte[] data;
    //name of the region file
    private final String fileName;

    public RegionFileData(byte[] data, String fileName) {
        this.data = data;
        this.fileName = fileName;
    }

    public byte[] getData() {
        return this.data;
    }

    public String getFileName() {
        return this.fileName;
    }
}
