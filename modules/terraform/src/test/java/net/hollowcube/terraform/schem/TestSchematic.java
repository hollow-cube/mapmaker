package net.hollowcube.terraform.schem;

import kotlin.Pair;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestSchematic {

    @Test
    void applyNoRotation() {
        var schem = new Schematic(
                new Vec(2, 2, 2),
                new Vec(2, 0, 0),
                new Block[]{Block.OAK_LOG, Block.BLACK_WOOL},
                new byte[]{0, 0, 0, 0, 1, 1, 1, 1}
        );
        var result = new ArrayList<Pair<Point, Block>>();
        schem.apply(Rotation.NONE, (pos, block) -> result.add(new Pair<>(pos, block)));

        assertEquals(new Pair<>(new Vec(2, 0, 0), Block.OAK_LOG), result.get(0));
        assertEquals(new Pair<>(new Vec(3, 0, 0), Block.OAK_LOG), result.get(1));
        assertEquals(new Pair<>(new Vec(2, 0, 1), Block.OAK_LOG), result.get(2));
        assertEquals(new Pair<>(new Vec(3, 0, 1), Block.OAK_LOG), result.get(3));
        assertEquals(new Pair<>(new Vec(2, 1, 0), Block.BLACK_WOOL), result.get(4));
        assertEquals(new Pair<>(new Vec(3, 1, 0), Block.BLACK_WOOL), result.get(5));
        assertEquals(new Pair<>(new Vec(2, 1, 1), Block.BLACK_WOOL), result.get(6));
        assertEquals(new Pair<>(new Vec(3, 1, 1), Block.BLACK_WOOL), result.get(7));
    }

    @Test
    void applyRotate90() {
        var schem = new Schematic(
                new Vec(2, 2, 2),
                new Vec(2, 0, 0),
                new Block[]{Block.OAK_LOG, Block.BLACK_WOOL},
                new byte[]{0, 0, 0, 0, 1, 1, 1, 1}
        );
        var result = new ArrayList<Pair<Point, Block>>();
        schem.apply(Rotation.CLOCKWISE_90, (pos, block) -> result.add(new Pair<>(pos, block)));

        assertEquals(new Pair<>(new Vec(0, 0, 2), Block.OAK_LOG), result.get(0));
        assertEquals(new Pair<>(new Vec(0, 0, 3), Block.OAK_LOG), result.get(1));
        assertEquals(new Pair<>(new Vec(-1, 0, 2), Block.OAK_LOG), result.get(2));
        assertEquals(new Pair<>(new Vec(-1, 0, 3), Block.OAK_LOG), result.get(3));
        assertEquals(new Pair<>(new Vec(0, 1, 2), Block.BLACK_WOOL), result.get(4));
        assertEquals(new Pair<>(new Vec(0, 1, 3), Block.BLACK_WOOL), result.get(5));
        assertEquals(new Pair<>(new Vec(-1, 1, 2), Block.BLACK_WOOL), result.get(6));
        assertEquals(new Pair<>(new Vec(-1, 1, 3), Block.BLACK_WOOL), result.get(7));
    }

}
