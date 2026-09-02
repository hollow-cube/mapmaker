package net.hollowcube.anticheat.state;

import net.hollowcube.anticheat.protocol.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StateCacheTest {

    private static final int ZOMBIE = 151;
    private static final int ADD_ENTITY = playId("add_entity");
    private static final int SET_ENTITY_DATA = playId("set_entity_data");
    private static final int SET_PASSENGERS = playId("set_passengers");
    private static final int CONTAINER_SET_SLOT = playId("container_set_slot");
    private static final int SET_HEALTH = playId("set_health");
    private static final int GAME_EVENT = playId("game_event");
    private static final int SET_PLAYER_TEAM = playId("set_player_team");
    private static final int PLAYER_INFO_UPDATE = playId("player_info_update");
    private static final int PLAYER_INFO_REMOVE = playId("player_info_remove");

    @Test
    void testEntityKeysAreLastWinsAndDropOnRemove() {
        var cache = new StateCache();
        feed(cache, add(7, ZOMBIE));
        feed(cache, add(8, ZOMBIE));
        feed(cache, new S2CUpdateMobEffect.V776(7, 1, 0, 200, (byte) 0));
        feed(cache, new S2CUpdateMobEffect.V776(7, 2, 0, 200, (byte) 0));

        assertNotNull(cache.frame(new StateKey.Entity(7, ADD_ENTITY)));
        assertNotNull(cache.frame(new StateKey.Effect(7, 1)));
        assertNotNull(cache.frame(new StateKey.Effect(7, 2)));

        feed(cache, new S2CRemoveMobEffect.V776(7, 1));
        assertNull(cache.frame(new StateKey.Effect(7, 1)));
        assertNotNull(cache.frame(new StateKey.Effect(7, 2)));

        feed(cache, new S2CRemoveEntities.V776(new int[]{7}));
        assertNull(cache.frame(new StateKey.Entity(7, ADD_ENTITY)));
        assertNull(cache.frame(new StateKey.Effect(7, 2)));
        assertNotNull(cache.frame(new StateKey.Entity(8, ADD_ENTITY)), "the other entity is untouched");
    }

    @Test
    void testEntityDataAccumulatesInOrder() {
        var cache = new StateCache();
        feed(cache, add(7, ZOMBIE));
        feed(cache, new S2CSetEntityData.V776(7, new byte[]{1}));
        feed(cache, new S2CSetEntityData.V776(7, new byte[]{2}));

        var frames = cache.frames(new StateKey.Entity(7, SET_ENTITY_DATA));
        assertEquals(2, frames.size());
        assertArrayEquals(new byte[]{7, 1}, frames.get(0).body());
        assertArrayEquals(new byte[]{7, 2}, frames.get(1).body());

        feed(cache, new S2CRemoveEntities.V776(new int[]{7}));
        assertEquals(List.of(), cache.frames(new StateKey.Entity(7, SET_ENTITY_DATA)));
    }

    @Test
    void testContainerSetContentResetsThatContainersSlots() {
        var cache = new StateCache();
        feed(cache, new S2CContainerSetSlot.V776(1, slotBody(3)), CONTAINER_SET_SLOT);
        feed(cache, new S2CContainerSetSlot.V776(1, slotBody(4)), CONTAINER_SET_SLOT);
        feed(cache, new S2CContainerSetSlot.V776(2, slotBody(3)), CONTAINER_SET_SLOT);
        assertNotNull(cache.frame(new StateKey.ContainerSlot(1, 3)));

        feed(cache, new S2CContainerSetContent.V776(1, new byte[]{9}));

        assertNull(cache.frame(new StateKey.ContainerSlot(1, 3)));
        assertNull(cache.frame(new StateKey.ContainerSlot(1, 4)));
        assertNotNull(cache.frame(new StateKey.ContainerSlot(2, 3)), "another container is untouched");
        assertNotNull(cache.frame(new StateKey.Container(1, playId("container_set_content"))));
    }

    @Test
    void testDisplayEntitiesAreNotCachedUntilTheyArePromoted() {
        var cache = new StateCache();
        feed(cache, add(7, EntityTypes776.BLOCK_DISPLAY));
        feed(cache, new S2CSetEntityData.V776(7, new byte[]{1}));

        assertTrue(cache.entities().isDropped(7));
        assertNull(cache.frame(new StateKey.Entity(7, ADD_ENTITY)));
        assertEquals(List.of(), cache.frames(new StateKey.Entity(7, SET_ENTITY_DATA)));
        assertEquals(0, cache.keyCount());

        // The local player boards it: the vehicle is a dropped display, the passenger is not.
        feed(cache, new S2CSetPassengers.V776(7, new int[]{42}));

        assertFalse(cache.entities().isDropped(7));
        var synthesized = cache.frame(new StateKey.Entity(7, ADD_ENTITY));
        assertNotNull(synthesized, "promotion emits a synthesized add_entity");
        var spawn = S2CAddEntity.V776.decode(new ByteReader(synthesized.body()));
        assertEquals(EntityTypes776.BLOCK_DISPLAY, spawn.entityTypeId());
        assertEquals(1.0, spawn.x());
        assertNotNull(cache.frame(new StateKey.Entity(7, SET_PASSENGERS)));

        feed(cache, new S2CSetEntityData.V776(7, new byte[]{2}));
        assertEquals(1, cache.frames(new StateKey.Entity(7, SET_ENTITY_DATA)).size(), "kept from now on");
    }

    @Test
    void testADisplayCarryingOnlyDisplaysIsNotPromoted() {
        var cache = new StateCache();
        feed(cache, add(7, EntityTypes776.TEXT_DISPLAY));
        feed(cache, add(8, EntityTypes776.ITEM_DISPLAY));

        feed(cache, new S2CSetPassengers.V776(7, new int[]{8}));

        assertTrue(cache.entities().isDropped(7));
        assertEquals(0, cache.keyCount());
    }

    @Test
    void testUndecodedPacketsAreKeyedByTheirPrefix() {
        var cache = new StateCache();
        feedRaw(cache, SET_HEALTH, new byte[]{0, 0, 0, 0, 5, 0, 0, 0, 0});
        feedRaw(cache, GAME_EVENT, new byte[]{3, 0, 0, 0, 0});
        feedRaw(cache, GAME_EVENT, new byte[]{7, 0, 0, 0, 0});
        feedRaw(cache, GAME_EVENT, new byte[]{3, 1, 1, 1, 1});

        assertNotNull(cache.frame(new StateKey.Singleton(SET_HEALTH)));
        assertEquals(2, cache.keyCount() - 1, "one key per game event type");
        assertArrayEquals(new byte[]{3, 1, 1, 1, 1}, cache.frame(new StateKey.Singleton(GAME_EVENT, 3)).body());
        assertNotNull(cache.frame(new StateKey.Singleton(GAME_EVENT, 7)));
    }

    @Test
    void testTeamsAreKeyedByNameAndRemovedByMethodOne() {
        var cache = new StateCache();
        feedRaw(cache, SET_PLAYER_TEAM, team("red", 0));
        feedRaw(cache, SET_PLAYER_TEAM, team("blue", 0));
        assertNotNull(cache.frame(new StateKey.Team("red")));

        feedRaw(cache, SET_PLAYER_TEAM, team("red", 1));

        assertNull(cache.frame(new StateKey.Team("red")));
        assertNotNull(cache.frame(new StateKey.Team("blue")));
    }

    @Test
    void testPlayerInfoIsSplitPerProfileAndResetOnAdd() {
        var cache = new StateCache();
        var first = new UUID(1, 1);
        var second = new UUID(2, 2);

        feedRaw(cache, PLAYER_INFO_UPDATE, latency(first, second));
        feedRaw(cache, PLAYER_INFO_UPDATE, latency(first, second));
        assertEquals(2, cache.frames(new StateKey.PlayerInfo(first)).size());
        assertEquals(2, cache.frames(new StateKey.PlayerInfo(second)).size());

        feedRaw(cache, PLAYER_INFO_UPDATE, addPlayer(first, "alice"));
        assertEquals(1, cache.frames(new StateKey.PlayerInfo(first)).size(), "add_player resets that profile");
        assertEquals(2, cache.frames(new StateKey.PlayerInfo(second)).size());

        feedRaw(cache, PLAYER_INFO_REMOVE, new ByteWriter().varInt(1).uuid(second).toByteArray());
        assertEquals(List.of(), cache.frames(new StateKey.PlayerInfo(second)));
        assertEquals(1, cache.frames(new StateKey.PlayerInfo(first)).size());
    }

    @Test
    void testUnsplittablePlayerInfoFallsBackToOneBucket() {
        var cache = new StateCache();
        feedRaw(cache, PLAYER_INFO_UPDATE, new byte[]{(byte) 0xFF, 1, 0, 0});

        assertEquals(1, cache.frames(new StateKey.PlayerInfo(null)).size());
    }

    @Test
    void testLoginResetsPlayStateButKeepsTheConfigurationSet() {
        var cache = new StateCache();
        cache.apply(ProtocolState.CONFIGURATION, Direction.S2C,
            Protocol776.packetId(ProtocolState.CONFIGURATION, Direction.S2C, "registry_data"),
            new S2CRegistryData.V776("minecraft:dimension_type", List.of()).toByteArray(), null);
        feed(cache, add(7, ZOMBIE));
        feedRaw(cache, SET_HEALTH, new byte[]{0, 0, 0, 0, 5, 0, 0, 0, 0});
        assertEquals(1, cache.frames(StateKey.Config.INSTANCE).size());

        feed(cache, login("minecraft:overworld"));

        assertEquals(1, cache.frames(StateKey.Config.INSTANCE).size(), "the configuration set survives");
        assertNull(cache.frame(new StateKey.Entity(7, ADD_ENTITY)));
        assertNull(cache.frame(new StateKey.Singleton(SET_HEALTH)));
        assertNotNull(cache.frame(new StateKey.Login(playId("login"))));
        assertEquals(0, cache.entities().size());
    }

    @Test
    void testRespawnOnlyDropsEntitiesWhenTheDimensionChanges() {
        var cache = new StateCache();
        feed(cache, login("minecraft:overworld"));
        feed(cache, add(7, ZOMBIE));
        feed(cache, new S2CContainerSetSlot.V776(1, slotBody(3)), CONTAINER_SET_SLOT);

        feed(cache, new S2CRespawn.V776(spawnInfo("minecraft:overworld"), (byte) 0));
        assertNotNull(cache.frame(new StateKey.Entity(7, ADD_ENTITY)), "same dimension keeps the level");
        assertNull(cache.frame(new StateKey.ContainerSlot(1, 3)), "but the container is always closed");

        feed(cache, new S2CRespawn.V776(spawnInfo("minecraft:the_nether"), (byte) 0));
        assertNull(cache.frame(new StateKey.Entity(7, ADD_ENTITY)));
        assertEquals(0, cache.entities().size());
    }

    @Test
    void testRespawnIntoAnotherDimensionKeepsThePlayersAttributesAsTheClientDoes() {
        var cache = new StateCache();
        feed(cache, login("minecraft:overworld"));
        feed(cache, add(7, ZOMBIE));
        int updateAttributes = playId("update_attributes");
        var sprinting = new S2CUpdateAttributes.Modifier("minecraft:sprinting", 0.3, 2);
        var speed = new S2CUpdateAttributes.Snapshot(26, 0.1, List.of(sprinting));
        var waterWalker = new S2CUpdateAttributes.Snapshot(37, 1.0 / 3.0, List.of());
        feed(cache, new S2CUpdateAttributes.V776(42, List.of(speed, waterWalker)), updateAttributes);
        feed(cache, new S2CUpdateAttributes.V776(7, List.of(speed)), updateAttributes);
        feed(cache, new S2CSetEntityData.V776(42, new byte[]{1}));

        feed(cache, new S2CRespawn.V776(spawnInfo("mapmaker:map/a"), (byte) 0));
        assertNull(cache.frame(new StateKey.EntityAttribute(7, 26)), "the other entities go with the level");
        var kept = cache.frame(new StateKey.EntityAttribute(42, 26));
        assertNotNull(kept, "assignBaseValues carries the player's bases into the new player");
        var base = new S2CUpdateAttributes.Snapshot(26, 0.1, List.of());
        assertArrayEquals(new S2CUpdateAttributes.V776(42, List.of(base)).toByteArray(), kept.body(), "without its modifiers");
        assertNotNull(cache.frame(new StateKey.EntityAttribute(42, 37)));
        assertTrue(cache.frames(new StateKey.Entity(42, SET_ENTITY_DATA)).isEmpty(), "a new LocalPlayer's data is fresh without KEEP_ENTITY_DATA");
        assertNotNull(cache.frame(new StateKey.Login(playId("respawn"))));

        feed(cache, new S2CUpdateAttributes.V776(42, List.of(speed)), updateAttributes);
        feed(cache, new S2CSetEntityData.V776(42, new byte[]{2}));
        feed(cache, new S2CRespawn.V776(spawnInfo("mapmaker:map/b"),
            (byte) (S2CRespawn.KEEP_ATTRIBUTE_MODIFIERS | S2CRespawn.KEEP_ENTITY_DATA)));
        assertArrayEquals(new S2CUpdateAttributes.V776(42, List.of(speed)).toByteArray(),
            cache.frame(new StateKey.EntityAttribute(42, 26)).body(), "assignAllValues keeps the modifiers");
        assertEquals(1, cache.frames(new StateKey.Entity(42, SET_ENTITY_DATA)).size());
    }

    @Test
    void testStartConfigurationResetsEverything() {
        var cache = new StateCache();
        cache.apply(ProtocolState.CONFIGURATION, Direction.S2C,
            Protocol776.packetId(ProtocolState.CONFIGURATION, Direction.S2C, "update_tags"), new byte[]{0}, null);
        feed(cache, login("minecraft:overworld"));
        feed(cache, add(7, ZOMBIE));

        feed(cache, new S2CStartConfiguration.V776());

        assertEquals(0, cache.keyCount());
        assertEquals(0, cache.entities().size());
    }

    @Test
    void testSnapshotIsUnchangedByLaterWrites() {
        var cache = new StateCache();
        feed(cache, add(7, ZOMBIE));
        feed(cache, new S2CSetEntityData.V776(7, new byte[]{1}));

        var view = cache.snapshot();
        feed(cache, add(8, ZOMBIE));
        feed(cache, new S2CSetEntityData.V776(7, new byte[]{2}));
        feed(cache, new S2CRemoveEntities.V776(new int[]{7}));

        assertNotNull(view.frame(new StateKey.Entity(7, ADD_ENTITY)));
        assertNull(view.frame(new StateKey.Entity(8, ADD_ENTITY)));
        assertEquals(1, view.frames(new StateKey.Entity(7, SET_ENTITY_DATA)).size());
        assertEquals(2, view.frames().size());
        assertNull(cache.frame(new StateKey.Entity(7, ADD_ENTITY)));
        assertNotNull(cache.frame(new StateKey.Entity(8, ADD_ENTITY)));
    }

    @Test
    void testFramesComeBackInArrivalOrder() {
        var cache = new StateCache();
        feed(cache, login("minecraft:overworld"));
        feed(cache, add(7, ZOMBIE));
        feed(cache, new S2CSetEntityData.V776(7, new byte[]{1}));
        feedRaw(cache, SET_HEALTH, new byte[]{0, 0, 0, 0, 5, 0, 0, 0, 0});
        feed(cache, new S2CSetEntityData.V776(7, new byte[]{2}));

        var frames = cache.snapshot().frames();
        assertEquals(List.of(playId("login"), ADD_ENTITY, SET_ENTITY_DATA, SET_HEALTH, SET_ENTITY_DATA),
            frames.stream().map(StateFrame::packetId).toList());
        for (int i = 1; i < frames.size(); i++)
            assertTrue(frames.get(i - 1).sequence() < frames.get(i).sequence());
    }

    @Test
    void testClientBrandIsPicked() {
        var cache = new StateCache();
        byte[] payload = new ByteWriter().utf("vanilla").toByteArray();
        cache.apply(ProtocolState.PLAY, Direction.C2S, Protocol776.packetId(ProtocolState.PLAY, Direction.C2S, "custom_payload"),
            new byte[0], new C2SCustomPayload.V776("minecraft:brand", payload));

        assertEquals("vanilla", cache.brand());
    }

    @Test
    void testAttributesAreKeptOneByOneAcrossPackets() {
        var cache = new StateCache();
        feed(cache, add(7, ZOMBIE));
        var speed = new S2CUpdateAttributes.Snapshot(26, 0.1, List.of());
        var gravity = new S2CUpdateAttributes.Snapshot(14, 0.08, List.of());
        var scale = new S2CUpdateAttributes.Snapshot(28, 2.0, List.of());
        int updateAttributes = playId("update_attributes");
        feed(cache, new S2CUpdateAttributes.V776(7, List.of(speed, gravity)), updateAttributes);
        feed(cache, new S2CUpdateAttributes.V776(7, List.of(scale)), updateAttributes);

        var kept = cache.frame(new StateKey.EntityAttribute(7, 26));
        assertNotNull(kept, "the speed set by the first packet survives the second");
        assertArrayEquals(new S2CUpdateAttributes.V776(7, List.of(speed)).toByteArray(), kept.body());
        assertNotNull(cache.frame(new StateKey.EntityAttribute(7, 14)));
        assertNotNull(cache.frame(new StateKey.EntityAttribute(7, 28)));
        assertNull(cache.frame(new StateKey.Entity(7, updateAttributes)));

        feed(cache, new S2CRemoveEntities.V776(new int[]{7}));
        assertNull(cache.frame(new StateKey.EntityAttribute(7, 26)));
    }

    private static void feed(StateCache cache, Packet packet) {
        feed(cache, packet, packetIdOf(packet));
    }

    private static void feed(StateCache cache, Packet packet, int packetId) {
        cache.apply(ProtocolState.PLAY, Direction.S2C, packetId, packet.toByteArray(), packet);
    }

    private static void feedRaw(StateCache cache, int packetId, byte[] body) {
        cache.apply(ProtocolState.PLAY, Direction.S2C, packetId, body, null);
    }

    private static int packetIdOf(Packet packet) {
        return switch (packet) {
            case S2CAddEntity.V776 _ -> ADD_ENTITY;
            case S2CSetEntityData.V776 _ -> SET_ENTITY_DATA;
            case S2CSetPassengers.V776 _ -> SET_PASSENGERS;
            case S2CUpdateMobEffect.V776 _ -> playId("update_mob_effect");
            case S2CRemoveMobEffect.V776 _ -> playId("remove_mob_effect");
            case S2CRemoveEntities.V776 _ -> playId("remove_entities");
            case S2CContainerSetContent.V776 _ -> playId("container_set_content");
            case S2CLogin.V776 _ -> playId("login");
            case S2CRespawn.V776 _ -> playId("respawn");
            case S2CStartConfiguration.V776 _ -> playId("start_configuration");
            default -> throw new IllegalArgumentException("no id for " + packet.getClass());
        };
    }

    private static S2CAddEntity.V776 add(int entityId, int typeId) {
        return new S2CAddEntity.V776(entityId, new UUID(0, entityId), typeId, 1.0, 2.0, 3.0, LpVec3.ZERO,
            (byte) 0, (byte) 0, (byte) 0, 0);
    }

    private static S2CLogin.V776 login(String dimension) {
        return new S2CLogin.V776(42, false, List.of(dimension), 20, 8, 8, false, true, false,
            spawnInfo(dimension), false, false);
    }

    private static CommonPlayerSpawnInfo spawnInfo(String dimension) {
        return new CommonPlayerSpawnInfo(0, dimension, 0L, 0, (byte) -1, false, false, null, 0, 63);
    }

    /// `container_set_slot` after its container id: a varint state id then a short slot.
    private static byte[] slotBody(int slot) {
        return new ByteWriter().varInt(0).i16(slot).toByteArray();
    }

    private static byte[] team(String name, int method) {
        return new ByteWriter().utf(name).u8(method).toByteArray();
    }

    /// One `player_info_update` carrying UPDATE_LATENCY for two profiles.
    private static byte[] latency(UUID... profiles) {
        var writer = new ByteWriter().u8(1 << 4).varInt(profiles.length);
        for (UUID profile : profiles) writer.uuid(profile).varInt(15);
        return writer.toByteArray();
    }

    /// One `player_info_update` carrying ADD_PLAYER with an empty property list.
    private static byte[] addPlayer(UUID profile, String name) {
        return new ByteWriter().u8(1).varInt(1).uuid(profile).utf(name).varInt(0).toByteArray();
    }

    private static int playId(String name) {
        return Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, name);
    }
}
