package net.hollowcube.terraform.buffer;

final class NaivePalette implements Palette {
    private static final int SIZE = 16;

    private final int[] palette = new int[SIZE * SIZE * SIZE];

    @Override
    public int get(int x, int y, int z) {
        return palette[getIndex(x, y, z)] - 1;
    }

    @Override
    public void set(int x, int y, int z, int value) {
        palette[getIndex(x, y, z)] = value + 1;
    }

    @Override
    public long sizeBytes() {
        return (long) palette.length * Integer.BYTES;
    }

    private static int getIndex(int x, int y, int z) {
        return x + SIZE * (y + SIZE * z);
    }
}
