package net.hollowcube.terraform.buffer;

public sealed interface Palette permits NaivePalette {
    int UNSET = -1;

    int get(int x, int y, int z);

    void set(int x, int y, int z, int value);

}
