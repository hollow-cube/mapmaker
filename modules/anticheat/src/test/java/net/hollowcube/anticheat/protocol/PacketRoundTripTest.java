package net.hollowcube.anticheat.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/// Golden bytes for every decoded 776 record, laid out by hand from the vanilla `STREAM_CODEC`.
/// Each case asserts the decoder consumes the whole frame and that re-encoding gives back exactly
/// the bytes it was handed — the property the capture format depends on.
class PacketRoundTripTest {

    @Test
    void testPingPong() {
        byte[] ping = new ByteWriter().i32(0x8000_0007).toByteArray();
        assertEquals(0x8000_0007, roundTrip(ping, S2CPing.V776::decode).id());
        assertEquals(-1, roundTrip(new ByteWriter().i32(-1).toByteArray(), C2SPong.V776::decode).id());
    }

    @Test
    void testMovePlayerPos() {
        byte[] bytes = new ByteWriter().f64(1.5).f64(64.0).f64(-2.5).u8(3).toByteArray();
        var packet = roundTrip(bytes, C2SMovePlayerPos.V776::decode);
        assertEquals(1.5, packet.x());
        assertEquals(-2.5, packet.z());
        assertTrue(packet.hasPosition());
        assertTrue(packet.onGround());
        assertTrue(packet.horizontalCollision());
    }

    @Test
    void testMovePlayerPosRotRotAndStatusOnly() {
        byte[] posRot = new ByteWriter().f64(1).f64(2).f64(3).f32(90).f32(-45).u8(1).toByteArray();
        var full = roundTrip(posRot, C2SMovePlayerPosRot.V776::decode);
        assertEquals(90F, full.yRot());
        assertTrue(full.hasRotation());
        assertTrue(full.onGround());
        assertEquals(false, full.horizontalCollision());

        var rot = roundTrip(new ByteWriter().f32(12).f32(-3).u8(0).toByteArray(), C2SMovePlayerRot.V776::decode);
        assertEquals(false, rot.hasPosition());
        assertEquals(0, rot.x());

        var status = roundTrip(new ByteWriter().u8(2).toByteArray(), C2SMovePlayerStatusOnly.V776::decode);
        assertEquals(false, status.onGround());
        assertTrue(status.horizontalCollision());
    }

    @Test
    void testPlayerPositionAndRotation() {
        var writer = new ByteWriter().varInt(42);
        writePositionMoveRotation(writer);
        byte[] bytes = writer.i32(Relative.X | Relative.ROTATE_DELTA).toByteArray();

        var packet = roundTrip(bytes, S2CPlayerPosition.V776::decode);
        assertEquals(42, packet.teleportId());
        assertEquals(10.5, packet.change().x());
        assertTrue(Relative.isSet(packet.relatives(), Relative.ROTATE_DELTA));
        assertEquals(false, Relative.isSet(packet.relatives(), Relative.Y));

        S2CPlayerRotation.V776 rotation = roundTrip(
            new ByteWriter().f32(1).bool(true).f32(2).bool(false).toByteArray(), S2CPlayerRotation.V776::decode);
        assertTrue(rotation.relativeYRot());
        assertEquals(false, rotation.relativeXRot());
    }

    @Test
    void testTeleportEntityAndPositionSync() {
        var teleport = new ByteWriter().varInt(7);
        writePositionMoveRotation(teleport);
        var packet = roundTrip(teleport.i32(0).bool(true).toByteArray(), S2CTeleportEntity.V776::decode);
        assertEquals(7, packet.entityId());
        assertTrue(packet.onGround());

        var sync = new ByteWriter().varInt(9);
        writePositionMoveRotation(sync);
        assertEquals(9, roundTrip(sync.bool(false).toByteArray(), S2CEntityPositionSync.V776::decode).entityId());
    }

    @Test
    void testAddEntityWithZeroAndPackedMovement() {
        var uuid = UUID.fromString("00000000-0000-4000-8000-000000000001");
        byte[] still = new ByteWriter()
            .varInt(1234).uuid(uuid).varInt(148)
            .f64(0.5).f64(70.0).f64(-8.25)
            .u8(0) // LpVec3 zero
            .u8(10).u8(-20).u8(30)
            .varInt(0)
            .toByteArray();
        var packet = roundTrip(still, S2CAddEntity.V776::decode);
        assertEquals(1234, packet.entityId());
        assertEquals(uuid, packet.uuid());
        assertEquals(148, packet.entityTypeId());
        assertEquals(70.0, packet.y());
        assertEquals(0, packet.movement().x());

        // markers 0b110: scale low bits 2, continuation set, so a varint scale tail follows.
        byte[] moving = new ByteWriter()
            .varInt(2).uuid(uuid).varInt(1)
            .f64(0).f64(0).f64(0)
            .u8(0x06).u8(0x11).i32(0x1234_5678).varInt(300)
            .u8(0).u8(0).u8(0)
            .varInt(0)
            .toByteArray();
        assertEquals(2, roundTrip(moving, S2CAddEntity.V776::decode).entityId());
    }

    @Test
    void testAnimateEntityEventAndSetEntityMotion() {
        var animate = roundTrip(new ByteWriter().varInt(77).u8(2).toByteArray(), S2CAnimate.V776::decode);
        assertEquals(77, animate.entityId());
        assertEquals(S2CAnimate.WAKE_UP, animate.action());

        // The one play packet whose entity id is a plain int, not a varint.
        var event = roundTrip(new ByteWriter().i32(77).u8(55).toByteArray(), S2CEntityEvent.V776::decode);
        assertEquals(77, event.entityId());
        assertEquals(S2CEntityEvent.SWAP_HANDS, event.event());

        var still = roundTrip(new ByteWriter().varInt(77).u8(0).toByteArray(), S2CSetEntityMotion.V776::decode);
        assertEquals(0, still.movement().x());

        var moving = roundTrip(new ByteWriter().varInt(77).u8(0x06).u8(0x11).i32(0x1234_5678).varInt(300).toByteArray(),
            S2CSetEntityMotion.V776::decode);
        assertEquals(77, moving.entityId());
    }

    @Test
    void testMoveEntityFamily() {
        S2CMoveEntityPos.V776 pos = roundTrip(
            new ByteWriter().varInt(5).i16(100).i16(-200).i16(300).bool(true).toByteArray(), S2CMoveEntityPos.V776::decode);
        assertEquals((short) -200, pos.deltaY());
        assertTrue(pos.hasPosition());
        assertEquals(false, pos.hasRotation());

        S2CMoveEntityPosRot.V776 posRot = roundTrip(
            new ByteWriter().varInt(5).i16(1).i16(2).i16(3).u8(64).u8(-64).bool(false).toByteArray(),
            S2CMoveEntityPosRot.V776::decode);
        assertEquals((byte) 64, posRot.yRot());

        S2CMoveEntityRot.V776 rot = roundTrip(
            new ByteWriter().varInt(5).u8(1).u8(2).bool(true).toByteArray(), S2CMoveEntityRot.V776::decode);
        assertEquals(0, rot.deltaX());
    }

    @Test
    void testRemoveEntitiesAndPassengers() {
        S2CRemoveEntities.V776 removed = roundTrip(
            new ByteWriter().varIntArray(new int[]{1, 2, 300}).toByteArray(), S2CRemoveEntities.V776::decode);
        assertArrayEquals(new int[]{1, 2, 300}, removed.entityIds());

        S2CSetPassengers.V776 passengers = roundTrip(
            new ByteWriter().varInt(9).varIntArray(new int[]{4}).toByteArray(), S2CSetPassengers.V776::decode);
        assertEquals(9, passengers.entityId());
        assertArrayEquals(new int[]{4}, passengers.passengerIds());
    }

    @Test
    void testMobEffects() {
        S2CUpdateMobEffect.V776 update = roundTrip(
            new ByteWriter().varInt(1).varInt(25).varInt(2).varInt(600).u8(5).toByteArray(), S2CUpdateMobEffect.V776::decode);
        assertEquals(1, update.entityId());
        assertEquals(25, update.effectId());
        assertEquals(600, update.durationTicks());

        S2CRemoveMobEffect.V776 remove = roundTrip(
            new ByteWriter().varInt(1).varInt(25).toByteArray(), S2CRemoveMobEffect.V776::decode);
        assertEquals(25, remove.effectId());
    }

    @Test
    void testLevelChunkWithLight() {
        var sections = new ByteWriter();
        for (int i = 0; i < 2; i++) sections.i16(0).i16(0).u8(0).varInt(0).u8(0).varInt(0);

        byte[] bytes = new ByteWriter()
            .i32(-3).i32(7)
            .varInt(1).varInt(1).varInt(2).i64(0).i64(1) // one heightmap of two longs
            .byteArray(sections.toByteArray())
            .varInt(0) // no block entities
            .bytes(new byte[]{0, 0, 0, 0, 0, 0}) // light payload, kept verbatim
            .toByteArray();

        var packet = roundTrip(bytes, S2CLevelChunkWithLight.V776::decode);
        assertEquals(-3, packet.chunkX());
        assertEquals(7, packet.chunkZ());
        assertEquals(2, packet.sections().size());
        assertEquals(7, packet.blockEntitiesAndLight().length());
    }

    @Test
    void testBlockAndChunkCache() {
        long pos = Positions.blockPos(-100, 63, 200);
        var update = roundTrip(new ByteWriter().blockPos(pos).varInt(1234).toByteArray(), S2CBlockUpdate.V776::decode);
        assertEquals(-100, Positions.blockX(update.packedPos()));
        assertEquals(63, Positions.blockY(update.packedPos()));
        assertEquals(1234, update.blockStateId());

        long entry = (long) 4321 << 12 | (3 << 8 | 5 << 4 | 9);
        S2CSectionBlocksUpdate.V776 section = roundTrip(
            new ByteWriter().sectionPos(Positions.sectionPos(1, 2, 3)).varInt(1).varLong(entry).toByteArray(),
            S2CSectionBlocksUpdate.V776::decode);
        assertEquals(2, Positions.sectionY(section.packedSectionPos()));
        assertEquals(4321, S2CSectionBlocksUpdate.blockStateId(section.entries()[0]));
        assertEquals(3, S2CSectionBlocksUpdate.relativeX(section.entries()[0]));
        assertEquals(5, S2CSectionBlocksUpdate.relativeZ(section.entries()[0]));
        assertEquals(9, S2CSectionBlocksUpdate.relativeY(section.entries()[0]));

        S2CForgetLevelChunk.V776 forget = roundTrip(
            new ByteWriter().chunkPos(Positions.chunkPos(-4, 9)).toByteArray(), S2CForgetLevelChunk.V776::decode);
        assertEquals(-4, forget.chunkX());
        assertEquals(9, forget.chunkZ());

        assertEquals(12, roundTrip(new ByteWriter().varInt(12).varInt(-6).toByteArray(),
            S2CSetChunkCacheCenter.V776::decode).chunkX());
        assertEquals(8, roundTrip(new ByteWriter().varInt(8).toByteArray(), S2CSetChunkCacheRadius.V776::decode).radius());
    }

    @Test
    void testLoginAndRespawn() {
        ByteWriter writer = new ByteWriter()
            .i32(77).bool(false)
            .varInt(2).utf("minecraft:overworld").utf("minecraft:the_nether")
            .varInt(20).varInt(10).varInt(8)
            .bool(false).bool(true).bool(false);
        writeSpawnInfo(writer);
        byte[] bytes = writer.bool(true).bool(false).toByteArray();

        var login = roundTrip(bytes, S2CLogin.V776::decode);
        assertEquals(77, login.playerId());
        assertEquals(10, login.chunkRadius());
        assertEquals(2, login.levels().size());
        assertEquals(4, login.spawnInfo().dimensionTypeId());
        assertEquals("minecraft:overworld", login.spawnInfo().dimension());
        assertNull(login.spawnInfo().lastDeathLocation());

        var respawnWriter = new ByteWriter();
        writeSpawnInfo(respawnWriter);
        var respawn = roundTrip(respawnWriter.u8(3).toByteArray(), S2CRespawn.V776::decode);
        assertEquals((byte) 3, respawn.dataToKeep());
    }

    @Test
    void testLastDeathLocationIsOptional() {
        ByteWriter writer = new ByteWriter()
            .varInt(0).utf("minecraft:overworld").i64(1).u8(0).u8(-1).bool(false).bool(true)
            .bool(true).utf("minecraft:the_end").blockPos(Positions.blockPos(1, 2, 3))
            .varInt(0).varInt(63);
        var respawn = roundTrip(writer.u8(0).toByteArray(), S2CRespawn.V776::decode);
        assertEquals("minecraft:the_end", respawn.spawnInfo().lastDeathLocation().dimension());
    }

    @Test
    void testEmptyPackets() {
        assertArrayEquals(new byte[0], roundTrip(new byte[0], S2CStartConfiguration.V776::decode).toByteArray());
        assertArrayEquals(new byte[0], roundTrip(new byte[0], S2CFinishConfiguration.V776::decode).toByteArray());
        assertArrayEquals(new byte[0], roundTrip(new byte[0], C2SFinishConfiguration.V776::decode).toByteArray());
        assertArrayEquals(new byte[0], roundTrip(new byte[0], C2SConfigurationAcknowledged.V776::decode).toByteArray());
        assertArrayEquals(new byte[0], roundTrip(new byte[0], S2CBundleDelimiter.V776::decode).toByteArray());
    }

    @Test
    void testCustomPayload() {
        byte[] brandBytes = new ByteWriter().utf(CustomPayload.BRAND_CHANNEL).utf("vanilla").toByteArray();
        var brand = roundTrip(brandBytes, C2SCustomPayload.V776::decode);
        assertEquals("vanilla", brand.brand());

        byte[] otherBytes = new ByteWriter().utf("mapmaker:anticheat").bytes(new byte[]{7, 7}).toByteArray();
        var other = roundTrip(otherBytes, S2CCustomPayload.V776::decode);
        assertNull(other.brand());
        assertArrayEquals(new byte[]{7, 7}, other.payload());
    }

    @Test
    void testRegistryData() {
        byte[] nbt = new ByteWriter().u8(10).u8(3).i16(1).bytes("h".getBytes(java.nio.charset.StandardCharsets.UTF_8))
            .i32(384).u8(0).toByteArray();
        byte[] bytes = new ByteWriter()
            .utf(S2CRegistryData.DIMENSION_TYPE_REGISTRY)
            .varInt(2)
            .utf("minecraft:overworld").bool(true).bytes(nbt)
            .utf("minecraft:the_nether").bool(false)
            .toByteArray();

        var packet = roundTrip(bytes, S2CRegistryData.V776::decode);
        assertEquals(S2CRegistryData.DIMENSION_TYPE_REGISTRY, packet.registry());
        assertEquals("minecraft:overworld", packet.entries().get(0).id());
        assertArrayEquals(nbt, packet.entries().get(0).data());
        assertNull(packet.entries().get(1).data());
    }

    @Test
    void testUpdateTags() {
        byte[] bytes = new ByteWriter()
            .varInt(1)
            .utf("minecraft:block")
            .varInt(2)
            .utf("minecraft:climbable").varIntArray(new int[]{1, 2})
            .utf("minecraft:leaves").varIntArray(new int[]{})
            .toByteArray();

        var packet = roundTrip(bytes, S2CUpdateTags.V776::decode);
        assertEquals("minecraft:block", packet.registries().get(0).registry());
        assertEquals(2, packet.registries().get(0).tags().size());
        assertArrayEquals(new int[]{1, 2}, packet.registries().get(0).tags().get(0).entries());
    }

    @Test
    void testPrefixKeyedPackets() {
        byte[] metadata = new ByteWriter().varInt(31).bytes(new byte[]{0, 0, 1, (byte) 255}).toByteArray();
        assertEquals(31, roundTrip(metadata, S2CSetEntityData.V776::decode).entityId());

        byte[] equipment = new ByteWriter().varInt(31).bytes(new byte[]{0, 0}).toByteArray();
        assertEquals(31, roundTrip(equipment, S2CSetEquipment.V776::decode).entityId());

        byte[] attributes = new ByteWriter().varInt(31).varInt(1)
            .varInt(26).f64(0.10000000149011612).varInt(1).utf("minecraft:sprinting").f64(0.3).varInt(2)
            .toByteArray();
        var update = roundTrip(attributes, S2CUpdateAttributes.V776::decode);
        assertEquals(31, update.entityId());
        assertEquals(List.of(new S2CUpdateAttributes.Snapshot(26, 0.10000000149011612,
            List.of(new S2CUpdateAttributes.Modifier("minecraft:sprinting", 0.3, S2CUpdateAttributes.ADD_MULTIPLIED_TOTAL)))), update.attributes());

        byte[] link = new ByteWriter().i32(31).i32(-1).toByteArray();
        assertEquals(31, roundTrip(link, S2CSetEntityLink.V776::decode).entityId());

        byte[] slot = new ByteWriter().varInt(2).bytes(new byte[]{1, 0, 5, 0}).toByteArray();
        assertEquals(2, roundTrip(slot, S2CContainerSetSlot.V776::decode).containerId());

        byte[] content = new ByteWriter().varInt(2).bytes(new byte[]{1, 0}).toByteArray();
        assertEquals(2, roundTrip(content, S2CContainerSetContent.V776::decode).containerId());

        byte[] inventory = new ByteWriter().varInt(36).bytes(new byte[]{0}).toByteArray();
        assertEquals(36, roundTrip(inventory, S2CSetPlayerInventory.V776::decode).slot());
    }

    private static void writePositionMoveRotation(ByteWriter writer) {
        writer.f64(10.5).f64(64.0).f64(-3.25).f64(0.1).f64(-0.0784).f64(0).f32(180).f32(-12.5F);
    }

    private static void writeSpawnInfo(ByteWriter writer) {
        writer.varInt(4).utf("minecraft:overworld").i64(1234567890L)
            .u8(0).u8(-1).bool(false).bool(true)
            .bool(false)
            .varInt(0).varInt(63);
    }

    private static <T extends Packet> T roundTrip(byte[] bytes, Function<ByteReader, T> decoder) {
        var reader = new ByteReader(bytes);
        var packet = decoder.apply(reader);
        assertEquals(0, reader.remaining(), "decoder left bytes unread");
        assertArrayEquals(bytes, packet.toByteArray());
        return packet;
    }
}
