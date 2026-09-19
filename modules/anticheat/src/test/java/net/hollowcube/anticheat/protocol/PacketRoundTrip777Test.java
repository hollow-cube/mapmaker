package net.hollowcube.anticheat.protocol;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/// Golden bytes for the records 777 (26.3) introduced, laid out by hand from the 26.3 `STREAM_CODEC`s
/// the same way [PacketRoundTripTest] does for 776. Real-client 777 fixtures do not exist yet, so
/// these are the only check the layouts get.
class PacketRoundTrip777Test {

    @Test
    void testMoveEntityPosLinear() {
        // properties: on ground, no steps.
        byte[] bytes = new ByteWriter().varInt(5).varInt(1).i16(100).i16(-200).i16(300).toByteArray();
        var packet = roundTrip(bytes, S2CMoveEntityPos.V777::decode);
        assertEquals(5, packet.entityId());
        assertTrue(packet.onGround());
        assertInstanceOf(VecDelta.Linear.class, packet.delta());
        assertEquals(-200, packet.deltaY());
        assertTrue(packet.hasPosition());
        assertFalse(packet.hasRotation());
    }

    /// Each step is relative to the one before it, so the whole move is their sum — which can leave
    /// the `short` range a single delta has.
    @Test
    void testMoveEntityPosStepped() {
        byte[] bytes = new ByteWriter().varInt(5).varInt(3 << 1)
            .varInt(1).i16(30000).i16(0).i16(-1)
            .varInt(2).i16(30000).i16(4096).i16(-1)
            .varInt(3).i16(30000).i16(0).i16(-1)
            .toByteArray();
        var packet = roundTrip(bytes, S2CMoveEntityPos.V777::decode);
        assertFalse(packet.onGround());
        var stepped = assertInstanceOf(VecDelta.Stepped.class, packet.delta());
        assertEquals(3, stepped.stepCount());
        assertEquals(new VecDelta.Step(2, (short) 30000, (short) 4096, (short) -1), stepped.steps().get(1));
        assertEquals(90000, packet.deltaX());
        assertEquals(4096, packet.deltaY());
        assertEquals(-3, packet.deltaZ());
    }

    @Test
    void testMoveEntityPosRejectsAStepCountTheBytesCannotHold() {
        byte[] bytes = new ByteWriter().varInt(5).varInt(100 << 1).varInt(1).i16(0).i16(0).i16(0).toByteArray();
        assertThrows(ProtocolException.class, () -> S2CMoveEntityPos.V777.decode(new ByteReader(bytes)));
    }

    @Test
    void testMoveEntityPosRot() {
        byte[] linear = new ByteWriter().varInt(5).varInt(1).i16(1).i16(2).i16(3).u8(64).u8(-64).toByteArray();
        var packet = roundTrip(linear, S2CMoveEntityPosRot.V777::decode);
        assertTrue(packet.onGround());
        assertEquals(3, packet.deltaZ());
        assertEquals((byte) 64, packet.yRot());
        assertEquals((byte) -64, packet.xRot());

        byte[] stepped = new ByteWriter().varInt(5).varInt(1 << 1 | 1)
            .varInt(4).i16(10).i16(20).i16(30)
            .u8(1).u8(2)
            .toByteArray();
        var steppedPacket = roundTrip(stepped, S2CMoveEntityPosRot.V777::decode);
        assertEquals(20, steppedPacket.deltaY());
        assertEquals((byte) 2, steppedPacket.xRot());
    }

    @Test
    void testMoveEntityRotReadsOnGroundFirst() {
        var packet = roundTrip(new ByteWriter().varInt(5).bool(true).u8(1).u8(2).toByteArray(),
            S2CMoveEntityRot.V777::decode);
        assertTrue(packet.onGround());
        assertEquals((byte) 1, packet.yRot());
        assertEquals((byte) 2, packet.xRot());
        assertEquals(0, packet.deltaX());
        assertFalse(packet.hasPosition());
    }

    @Test
    void testEntityPositionSyncLinear() {
        byte[] bytes = new ByteWriter().varInt(9)
            .varInt(0).f64(1.5).f64(64).f64(-2.5)
            .f32(90).f32(-10).bool(true)
            .toByteArray();
        var packet = roundTrip(bytes, S2CEntityPositionSync.V777::decode);
        assertEquals(9, packet.entityId());
        assertEquals(1.5, packet.x());
        assertEquals(-2.5, packet.z());
        assertEquals(90F, packet.yRot());
        assertTrue(packet.onGround());
    }

    @Test
    void testEntityPositionSyncSteppedEndsAtTheLastStep() {
        byte[] bytes = new ByteWriter().varInt(9)
            .varInt(1).varInt(2)
            .f64(1).f64(2).f64(3).varInt(1)
            .f64(4).f64(5).f64(6).varInt(3)
            .f32(0).f32(0).bool(false)
            .toByteArray();
        var packet = roundTrip(bytes, S2CEntityPositionSync.V777::decode);
        var path = assertInstanceOf(PositionPath.Stepped.class, packet.path());
        assertEquals(new PositionPath.Step(1, 2, 3, 1), path.steps().getFirst());
        assertEquals(4, packet.x());
        assertEquals(5, packet.y());
        assertEquals(6, packet.z());
    }

    /// An empty stepped path has no end position, and an unknown type reads as linear on the client
    /// but could never re-encode to the same bytes; both are refused rather than misread.
    @Test
    void testEntityPositionSyncRejectsPathsItCannotRepresent() {
        byte[] empty = new ByteWriter().varInt(9).varInt(1).varInt(0).f32(0).f32(0).bool(false).toByteArray();
        assertThrows(ProtocolException.class, () -> S2CEntityPositionSync.V777.decode(new ByteReader(empty)));

        byte[] unknown = new ByteWriter().varInt(9).varInt(2).f64(0).f64(0).f64(0).f32(0).f32(0).bool(false)
            .toByteArray();
        assertThrows(ProtocolException.class, () -> S2CEntityPositionSync.V777.decode(new ByteReader(unknown)));
    }

    @Test
    void testAnimate() {
        var wake = roundTrip(new ByteWriter().varInt(77).u8(0).toByteArray(), S2CAnimate.V777::decode);
        assertTrue(wake.wakesUp());
        var crit = roundTrip(new ByteWriter().varInt(77).u8(2).toByteArray(), S2CAnimate.V777::decode);
        assertFalse(crit.wakesUp());
    }

    @Test
    void testLoginAndRespawnReadVarIntGameModes() {
        ByteWriter writer = new ByteWriter()
            .i32(77).bool(false)
            .varInt(1).utf("minecraft:overworld")
            .varInt(20).varInt(10).varInt(8)
            .bool(false).bool(true).bool(false);
        writeSpawnInfo(writer, 3, 0);
        byte[] bytes = writer.bool(true).bool(false).toByteArray();

        var login = roundTrip(bytes, S2CLogin.V777::decode);
        assertEquals(77, login.playerId());
        assertEquals(10, login.chunkRadius());
        assertEquals(4, login.spawnInfo().dimensionTypeId());
        assertEquals("minecraft:overworld", login.spawnInfo().dimension());
        assertEquals(3, login.spawnInfo().gameType());
        assertEquals(-1, login.spawnInfo().previousGameType(), "OPTIONAL_VAR_INT zero is no previous mode");

        var respawnWriter = new ByteWriter();
        writeSpawnInfo(respawnWriter, 1, 2);
        var respawn = roundTrip(respawnWriter.u8(3).toByteArray(), S2CRespawn.V777::decode);
        assertEquals(1, respawn.spawnInfo().previousGameType());
        assertEquals((byte) 3, respawn.dataToKeep());
    }

    /// 26.3 has more than 2^15 block states, so a direct section packs sixteen bits per entry: 1024
    /// longs, not the 1092 a fifteen bit section takes.
    @Test
    void testLevelChunkWithLightPacksDirectSectionsAtSixteenBits() {
        var data = new long[Section.longCount(16, Section.BLOCK_ENTRY_COUNT)];
        assertEquals(1024, data.length);
        data[0] = 35_000L | 2L << 16; // block 0 is state 35000, block 1 state 2
        var sections = new ByteWriter()
            .i16(4096).i16(0).u8(16).fixedLongArray(data)
            .u8(0).varInt(0);

        byte[] bytes = new ByteWriter()
            .i32(1).i32(2)
            .varInt(0)
            .byteArray(sections.toByteArray())
            .varInt(0)
            .toByteArray();

        var packet = roundTrip(bytes, S2CLevelChunkWithLight.V777::decode);
        var section = packet.sections().getFirst();
        assertEquals(1, packet.sections().size());
        assertEquals(35_000, section.get(0, 0, 0));
        assertEquals(2, section.get(1, 0, 0));
        assertEquals(S2CLevelChunkWithLight.V777.DIRECT_BLOCK_BITS, section.directBits());
    }

    private static void writeSpawnInfo(ByteWriter writer, int gameType, int previousGameTypePlusOne) {
        writer.varInt(4).utf("minecraft:overworld").i64(1234567890L)
            .varInt(gameType).varInt(previousGameTypePlusOne).bool(false).bool(true)
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
