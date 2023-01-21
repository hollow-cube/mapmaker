package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.selection.cui.MockSelectionRenderer;
import net.hollowcube.test.TestUtil;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestCuboidRegionSelector {
    private final Player mockPlayer = TestUtil.headlessPlayer();

    @Test
    public void testTwoPointSel() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(1, 1, 1));
        selector.selectSecondary(new Vec(2, 2, 2));

        var cuboid = assertInstanceOf(CuboidRegion.class, selector.region());
        //noinspection DataFlowIssue Intellij doesn't know what its talking about
        assertEquals(new Vec(1, 1, 1), cuboid.pos1());
        assertEquals(new Vec(2, 2, 2), cuboid.pos2());
    }

    @Test
    public void testPointReplacement() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(1, 1, 1));
        selector.selectSecondary(new Vec(2, 2, 2));

        selector.selectSecondary(new Vec(3, 3, 3));
        selector.selectPrimary(new Vec(4, 4, 4));

        var cuboid = assertInstanceOf(CuboidRegion.class, selector.region());
        //noinspection DataFlowIssue Intellij doesn't know what its talking about
        assertEquals(new Vec(4, 4, 4), cuboid.pos1());
        assertEquals(new Vec(3, 3, 3), cuboid.pos2());
    }

    @Test
    public void testCoordinateFloor() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(1.5, 1.5, 1.99999));
        selector.selectSecondary(new Vec(2.1, 2.5, 2.2));

        var cuboid = assertInstanceOf(CuboidRegion.class, selector.region());
        //noinspection DataFlowIssue Intellij doesn't know what its talking about
        assertEquals(new Vec(1, 1, 1), cuboid.pos1());
        assertEquals(new Vec(2, 2, 2), cuboid.pos2());
    }

    @Test
    public void testIncompleteSelection1() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(1, 1, 1));

        assertNull(selector.region());
    }

    @Test
    public void testIncompleteSelection2() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectSecondary(new Vec(2, 2, 2));

        assertNull(selector.region());
    }

    @Test
    public void testIncompleteSelection3() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(1, 1, 1));
        selector.selectSecondary(new Vec(2, 2, 2));
        selector.clear();

        assertNull(selector.region());
    }


    // Renderer/CUI

    @Test
    public void testNormalCuiUpdates() {
        //todo
    }

}
