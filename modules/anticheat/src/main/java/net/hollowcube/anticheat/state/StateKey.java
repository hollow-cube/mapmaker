package net.hollowcube.anticheat.state;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/// A [StateCache] key: whatever prefix of a packet identifies the thing it changes — an entity id,
/// a container id and slot, an effect id, a team name, a profile uuid.
///
/// Whether a key holds one packet or every packet since it was created is a property of the packet,
/// not of the key, and lives in the cache's two maps rather than here.
public sealed interface StateKey {

    /// The whole configuration phase, replaced wholesale by the next one.
    record Config() implements StateKey {

        public static final Config INSTANCE = new Config();
    }

    /// The last `login` and the last `respawn`, which are separate keys because a respawn does not
    /// replace the login.
    record Login(int packetId) implements StateKey {
    }

    /// One packet per id, with [#discriminator()] splitting the ids that carry several unrelated
    /// pieces of state behind one packet (`game_event`).
    record Singleton(int packetId, int discriminator) implements StateKey {

        public Singleton(int packetId) {
            this(packetId, 0);
        }
    }

    record Entity(int entityId, int packetId) implements StateKey {
    }

    /// One attribute of one entity: `update_attributes` carries whichever instances changed, so a
    /// later packet naming one attribute must not evict the earlier one that set the rest.
    record EntityAttribute(int entityId, int attribute) implements StateKey {
    }

    record Effect(int entityId, int effectId) implements StateKey {
    }

    record Container(int containerId, int packetId) implements StateKey {
    }

    record ContainerSlot(int containerId, int slot) implements StateKey {
    }

    record InventorySlot(int slot) implements StateKey {
    }

    record Team(String name) implements StateKey {
    }

    record Cooldown(String group) implements StateKey {
    }

    /// Keyed by profile uuid, or by a null uuid for the whole packet when it could not be split.
    record PlayerInfo(@Nullable UUID uuid) implements StateKey {
    }
}
