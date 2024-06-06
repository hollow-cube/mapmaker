package com.sk89q.worldedit.util;

import com.sk89q.worldedit.math.Vector3;
import net.minestom.server.coordinate.Pos;

public record Location(double x, double y, double z) {

    public Location(Pos pos) {
        this(pos.x(), pos.y(), pos.z());
    }

    public Vector3 toVector() {
        return new Vector3(x, y, z); //todo
    }
}
