package net.hollowcube.world.compression;

public class CompressedWorldData {
    //the byte array that contains all compressed world files
    private final byte[] compressedData;
    //the original world data size; needed for decompression
    private final int originalSize;

    public CompressedWorldData(byte[] compressedData, int originalSize) {
        this.compressedData = compressedData;
        this.originalSize = originalSize;
    }

    public byte[] getCompressedData() {
        return this.compressedData;
    }

    public int getOriginalSize() {
        return this.originalSize;
    }
}
