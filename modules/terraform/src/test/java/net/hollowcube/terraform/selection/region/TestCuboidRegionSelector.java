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
        selector.selectPrimary(new Vec(1, 1, 1), false);
        selector.selectSecondary(new Vec(2, 2, 2), false);

        var cuboid = assertInstanceOf(CuboidRegion.class, selector.region());
        //noinspection DataFlowIssue Intellij doesn't know what its talking about
        assertEquals(new Vec(1, 1, 1), cuboid.pos1());
        assertEquals(new Vec(2, 2, 2), cuboid.pos2());
    }

    @Test
    public void testPointReplacement() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(1, 1, 1), false);
        selector.selectSecondary(new Vec(2, 2, 2), false);

        selector.selectSecondary(new Vec(3, 3, 3), false);
        selector.selectPrimary(new Vec(4, 4, 4), false);

        var cuboid = assertInstanceOf(CuboidRegion.class, selector.region());
        //noinspection DataFlowIssue Intellij doesn't know what its talking about
        assertEquals(new Vec(4, 4, 4), cuboid.pos1());
        assertEquals(new Vec(3, 3, 3), cuboid.pos2());
    }

    @Test
    public void testCoordinateFloor() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(1.5, 1.5, 1.99999), false);
        selector.selectSecondary(new Vec(2.1, 2.5, 2.2), false);

        var cuboid = assertInstanceOf(CuboidRegion.class, selector.region());
        //noinspection DataFlowIssue Intellij doesn't know what its talking about
        assertEquals(new Vec(1, 1, 1), cuboid.pos1());
        assertEquals(new Vec(2, 2, 2), cuboid.pos2());
    }

    @Test
    public void testIncompleteSelection1() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(1, 1, 1), false);

        assertNull(selector.region());
    }

    @Test
    public void testIncompleteSelection2() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectSecondary(new Vec(2, 2, 2), false);

        assertNull(selector.region());
    }

    @Test
    public void testIncompleteSelection3() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(1, 1, 1), false);
        selector.selectSecondary(new Vec(2, 2, 2), false);
        selector.clear();

        assertNull(selector.region());
    }

    @Test
    public void testSizeChange() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(5, 0, 5), false);
        selector.selectSecondary(new Vec(15, 10, 15), false);
        selector.changeSize(-3, false, true);

        assertEquals(8, selector.region().min().x());
        assertEquals(13, selector.region().max().x());
        assertEquals(0, selector.region().min().y());
        assertEquals(11, selector.region().max().y());
    }

    @Test
    public void testSizeChangeOver() {
        var selector = new CuboidRegionSelector(mockPlayer, new MockSelectionRenderer());
        selector.selectPrimary(new Vec(5, 0, 5), false);
        selector.selectSecondary(new Vec(15, 10, 15), false);
        selector.changeSize(-20, true, true);

        assertEquals(10, selector.region().min().x());
        assertEquals(11, selector.region().max().x());
        assertEquals(5, selector.region().min().y());
        assertEquals(6, selector.region().max().y());
    }


    // Renderer/CUI

    @Test
    public void testNormalCuiUpdates() {
        //todo
    }

}
