package com.sk89q.worldedit.math;

public record BlockVector2(int x, int z) {

    public static BlockVector2 at(int x, int z) {
        return new BlockVector2(x, z);
    }
}
