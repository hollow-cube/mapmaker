package net.hollowcube.terraform.compat.metabrush;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

import java.util.HashMap;
import java.util.Map;

public class k_0 {
    public Map<Point, Double> a = new HashMap();
    public double b = 0.0;
    public Map<Point, Block> c = new HashMap<>();

    public double a() {
        return this.b;
    }

    public void a(double d) {
        this.b = d;
    }

    public void a(Point blockVector3, double d) {
        this.a.put(blockVector3, d);
    }

    public void a(Point blockVector3, Block blockState) {
        this.c.put(blockVector3, blockState);
    }

    public Map<Point, Double> b() {
        return this.a;
    }

    public Map<Point, Block> c() {
        return this.c;
    }

    public Block a(Point blockVector3, Instance editSession) {
        return this.c.getOrDefault(blockVector3, editSession.getBlock(blockVector3));
    }
}
