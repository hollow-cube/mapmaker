package net.hollowcube.terraform.util;

@FunctionalInterface
public interface PosIntConsumer {
    void accept(int x, int y, int z, int value);
}
