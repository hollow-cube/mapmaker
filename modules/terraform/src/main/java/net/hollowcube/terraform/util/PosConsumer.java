package net.hollowcube.terraform.util;

@FunctionalInterface
public interface PosConsumer<T> {
    void accept(int x, int y, int z, T value);
}
