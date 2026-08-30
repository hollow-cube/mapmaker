package net.hollowcube.anticheat.state;

import net.hollowcube.anticheat.protocol.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityTableTest {

    private static final UUID UUID_ONE = new UUID(1, 2);
    private static final int ZOMBIE = 151;

    @Test
    void testAddEntityRecordsTypeAndPosition() {
        var table = new EntityTable();
        table.apply(add(7, ZOMBIE, 10.5, 64.0, -3.25, (byte) 64, (byte) 32));

        var entity = table.get(7);
        assertNotNull(entity);
        assertEquals(UUID_ONE, entity.uuid());
        assertEquals(ZOMBIE, entity.typeId());
        assertEquals(10.5, entity.x());
        assertEquals(90.0f, entity.yRot(), 1e-4, "64 of 256 is a quarter turn");
        assertEquals(45.0f, entity.xRot(), 1e-4);
        assertFalse(entity.dropped());
    }

    /// Metadata merges last-wins per index and survives moves; the player's own metadata lands on
    /// the player entry, which has no row in the table.
    @Test
    void testSetEntityDataMergesMetadataPerIndex() {
        var table = new EntityTable();
        table.apply(add(7, ZOMBIE, 0, 64, 0, (byte) 0, (byte) 0));

        byte[] sneaking = new ByteWriter().u8(0).varInt(0).u8(0x02).u8(Metadata776.TERMINATOR).toByteArray();
        byte[] sprintingAndPose = new ByteWriter()
            .u8(0).varInt(0).u8(0x08)
            .u8(6).varInt(20).varInt(3)
            .u8(Metadata776.TERMINATOR).toByteArray();
        table.apply(new S2CSetEntityData.V776(7, sneaking));
        table.apply(new S2CSetEntityData.V776(7, sprintingAndPose));
        table.apply(new S2CMoveEntityPos.V776(7, (short) 4096, (short) 0, (short) 0, true));

        var entity = table.get(7);
        assertNotNull(entity);
        assertEquals(2, entity.metadata().size());
        assertArrayEquals(new ByteWriter().u8(0).varInt(0).u8(0x08).toByteArray(), entity.metadata().get(0),
            "the later flags byte wins");
        assertNotNull(entity.metadata().get(6));

        table.apply(login(42));
        table.apply(new S2CSetEntityData.V776(42, sneaking));
        assertEquals(1, table.player().metadata().size());
    }

    @Test
    void testMoveEntityAppliesDeltasInQuarterThousandthBlocks() {
        var table = new EntityTable();
        table.apply(add(7, ZOMBIE, 10.0, 64.0, -3.0, (byte) 0, (byte) 0));

        table.apply(new S2CMoveEntityPos.V776(7, (short) 4096, (short) -2048, (short) 1, true));

        var entity = table.get(7);
        assertEquals(11.0, entity.x());
        assertEquals(63.5, entity.y());
        assertEquals(-3.0 + EntityTable.DELTA_UNIT, entity.z());
        assertTrue(entity.onGround());

        table.apply(new S2CMoveEntityPos.V776(7, (short) -4096, (short) 0, (short) 0, false));
        assertEquals(10.0, table.get(7).x());
        assertFalse(table.get(7).onGround());
    }

    @Test
    void testRotationOnlyMoveKeepsThePosition() {
        var table = new EntityTable();
        table.apply(add(7, ZOMBIE, 10.0, 64.0, -3.0, (byte) 0, (byte) 0));

        table.apply(new S2CMoveEntityRot.V776(7, (byte) -128, (byte) 0, true));

        assertEquals(10.0, table.get(7).x());
        assertEquals(-180.0f, table.get(7).yRot(), 1e-4);
    }

    @Test
    void testTeleportHonoursTheRelativeFlags() {
        var table = new EntityTable();
        table.apply(add(7, ZOMBIE, 10.0, 64.0, -3.0, (byte) 0, (byte) 0));

        table.apply(new S2CTeleportEntity.V776(7,
            new PositionMoveRotation(1.0, 2.0, 3.0, 0, 0, 0, 10.0f, 0), Relative.X | Relative.Z, true));

        var entity = table.get(7);
        assertEquals(11.0, entity.x(), "x is relative");
        assertEquals(2.0, entity.y(), "y is absolute");
        assertEquals(0.0, entity.z());
        assertEquals(10.0f, entity.yRot());
    }

    @Test
    void testPositionSyncIsAlwaysAbsolute() {
        var table = new EntityTable();
        table.apply(add(7, ZOMBIE, 10.0, 64.0, -3.0, (byte) 0, (byte) 0));

        table.apply(new S2CEntityPositionSync.V776(7,
            new PositionMoveRotation(1.0, 2.0, 3.0, 0, 0, 0, 5.0f, 6.0f), false));

        var entity = table.get(7);
        assertEquals(1.0, entity.x());
        assertEquals(3.0, entity.z());
        assertEquals(6.0f, entity.xRot());
    }

    @Test
    void testMovesForUnknownEntitiesAreIgnored() {
        var table = new EntityTable();
        table.apply(new S2CMoveEntityPos.V776(99, (short) 4096, (short) 0, (short) 0, true));
        assertEquals(0, table.size());
    }

    @Test
    void testRemoveEntitiesDropsTheEntries() {
        var table = new EntityTable();
        table.apply(add(7, ZOMBIE, 0, 0, 0, (byte) 0, (byte) 0));
        table.apply(add(8, ZOMBIE, 0, 0, 0, (byte) 0, (byte) 0));

        table.apply(new S2CRemoveEntities.V776(new int[]{7, 404}));

        assertNull(table.get(7));
        assertNotNull(table.get(8));
    }

    @Test
    void testDisplayEntitiesAreTrackedButDropped() {
        var table = new EntityTable();
        table.apply(add(7, EntityTypes776.TEXT_DISPLAY, 1.0, 2.0, 3.0, (byte) 0, (byte) 0));

        assertTrue(table.isDropped(7));
        assertFalse(table.isDropped(404), "an entity we never saw is not dropped");
        table.apply(new S2CMoveEntityPos.V776(7, (short) 4096, (short) 0, (short) 0, true));
        assertEquals(2.0, table.get(7).x(), "dropped entities still track a position");

        assertNotNull(table.promote(7));
        assertFalse(table.isDropped(7));
        assertNull(table.promote(7), "promoting twice does nothing");
    }

    @Test
    void testInteractionEntitiesAreKept() {
        var table = new EntityTable();
        table.apply(add(7, EntityTypes776.INTERACTION, 0, 0, 0, (byte) 0, (byte) 0));
        assertFalse(table.isDropped(7));
    }

    @Test
    void testOwnPositionComesFromBothDirections() {
        var table = new EntityTable();
        table.apply(login(42));
        assertEquals(42, table.player().entityId());

        table.apply(new S2CPlayerPosition.V776(1,
            new PositionMoveRotation(8.0, 70.0, -4.0, 0, 0, 0, 90.0f, 0), 0));
        assertEquals(8.0, table.player().x());
        assertEquals(90.0f, table.player().yRot());

        table.apply(new S2CPlayerPosition.V776(2,
            new PositionMoveRotation(1.0, 0, 0, 0, 0, 0, 0, 0), Relative.X | Relative.Y_ROT));
        assertEquals(9.0, table.player().x(), "relative x adds");
        assertEquals(0.0, table.player().y(), "absolute y replaces");
        assertEquals(90.0f, table.player().yRot());

        table.apply(new C2SMovePlayerPosRot.V776(3.0, 4.0, 5.0, 6.0f, 7.0f, 1));
        assertEquals(3.0, table.player().x());
        assertEquals(7.0f, table.player().xRot());
        assertTrue(table.player().onGround());
    }

    @Test
    void testSnapshotIsUnchangedByLaterWrites() {
        var table = new EntityTable();
        table.apply(add(7, ZOMBIE, 10.0, 64.0, -3.0, (byte) 0, (byte) 0));

        var view = table.snapshot();
        table.apply(new S2CMoveEntityPos.V776(7, (short) 4096, (short) 0, (short) 0, true));
        table.apply(add(8, ZOMBIE, 0, 0, 0, (byte) 0, (byte) 0));

        assertEquals(1, view.size());
        assertEquals(10.0, view.get(7).x());
        assertNull(view.get(8));
        assertEquals(11.0, table.get(7).x());
        assertEquals(2, table.size());
    }

    private static S2CAddEntity.V776 add(int entityId, int typeId, double x, double y, double z, byte yRot, byte xRot) {
        return new S2CAddEntity.V776(entityId, UUID_ONE, typeId, x, y, z, LpVec3.ZERO, xRot, yRot, yRot, 0);
    }

    private static S2CLogin.V776 login(int playerId) {
        return new S2CLogin.V776(playerId, false, List.of("minecraft:overworld"), 20, 8, 8,
            false, true, false,
            new CommonPlayerSpawnInfo(0, "minecraft:overworld", 0L, 0, (byte) -1, false, false, null, 0, 63),
            false, false);
    }
}
