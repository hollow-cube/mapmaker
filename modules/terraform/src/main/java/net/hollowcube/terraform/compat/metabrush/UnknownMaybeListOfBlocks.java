package net.hollowcube.terraform.compat.metabrush;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UnknownMaybeListOfBlocks {
    double c = 0.0;
    public Map<Point, Double> d = Collections.synchronizedMap(new HashMap<>());
    public Map<Point, Block> a = new HashMap<>();
    final Point e;
    public Point origin;

    public UnknownMaybeListOfBlocks() {
        this.e = Vec.ZERO;
        this.origin = Vec.ZERO;
    }

    public UnknownMaybeListOfBlocks(Point blockVector3) {
        this.e = Vec.ZERO;
        this.origin = blockVector3;
    }
}

