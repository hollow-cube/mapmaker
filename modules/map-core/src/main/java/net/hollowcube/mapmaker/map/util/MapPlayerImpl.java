package net.hollowcube.mapmaker.map.util;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.hollowcube.command.util.CommandHandlingPlayer;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

/**
 * Implements some interfaces.
 * <p>
 * Overrides the following behavior:
 * - Always set listed to false on tab list entries. They will be managed by the session manager.
 */
public abstract class MapPlayerImpl extends CommandHandlingPlayer implements PlayerVisibilityExtension {
    private static final Set<String> ENABLED_FLOOR_TEST_USERS = Set.of("ontal", "itmg", "notmattw", "sethprg");
    private static final Logger log = LoggerFactory.getLogger(MapPlayerImpl.class);

    private Function<Player, Visibility> visibilityFunc = null;

    // entity id -> visibility ordinal
    private Int2IntMap visibilityByEntity = new Int2IntArrayMap();

    private long lastCollideTick = -1;

    public MapPlayerImpl(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection playerConnection) {
        super(uuid, username, playerConnection);
    }

    @Override
    public void setVisibilityFunc(@Nullable Function<Player, Visibility> func) {
        this.visibilityFunc = func;
    }

    @Override
    public void updateVisibility() {
        if (visibilityFunc == null) return;
        getViewers().forEach(this::updateVisibility);
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);
        updateVisibility(player);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);
        visibilityByEntity.remove(player.getEntityId());
    }

    @Override
    public void remove(boolean permanent) {
        super.remove(permanent);
        visibilityByEntity.clear();
    }

    @Override
    public void sendPacketToViewers(@NotNull SendablePacket packet) {
        // We need to intercept the metadata packet to ensure the invisible flag is set if we are supposed to be invisible.
        if ((!(packet instanceof EntityMetaDataPacket metaPacket))) {
            super.sendPacketToViewers(packet);
            return;
        }

        // If the update isnt changing the base entity bitflags we can just forward it.
        var flagsEntry = metaPacket.entries().get(0);
        if (flagsEntry == null) {
            super.sendPacketToViewers(packet);
            return;
        }

        var newEntries = new HashMap<>(metaPacket.entries());
        var bits = ((Metadata.Entry<Byte>) flagsEntry).value().byteValue();
        bits |= 0x20;
        newEntries.put(0, Metadata.Byte(bits));
        var invisPacket = new EntityMetaDataPacket(metaPacket.entityId(), newEntries);

        // Forward the original or new packet depending if we are invisible to them
        for (var viewer : getViewers()) {
            viewer.sendPacket(visibilityByEntity.get(viewer.getEntityId()) != Visibility.VISIBLE.ordinal()
                    ? invisPacket : metaPacket);
        }
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        // This is some relatively temporary code to attempt to prevent a player from falling through the floor on spawn.
        // It is not intended to be a solid check against noclip in an anticheat.
        if (ENABLED_FLOOR_TEST_USERS.contains(getUsername().toLowerCase(Locale.ROOT))) {
            var deltaY = Vec.fromPoint(getPosition().sub(getPreviousPosition())).y();
            if (deltaY < 0) {
                var didCollideY = CollisionUtils.handlePhysics(getInstance(), getBoundingBox(), getPreviousPosition(),
                        new Vec(0, deltaY, 0), null, true).collisionY();
                if (didCollideY) {
                    teleport(getPreviousPosition());
                    log.info("Teleported {} to prevent falling through the floor", getUsername());
                    lastCollideTick = getAliveTicks();
                } else {
                    lastCollideTick = -1;
                }
            } else {
                lastCollideTick = -1;
            }
        }
    }

    private void updateVisibility(@NotNull Player other) {
        if (visibilityFunc == null) return;
        var old = Visibility.VALUES[visibilityByEntity.getOrDefault(other.getEntityId(), 0)];
        var current = visibilityFunc.apply(other);
        if (old == current) return; // Do nothing

        other.sendPacket(new BundlePacket());

        if (old != Visibility.VISIBLE) {
            sendMetaInvisUpdate(other, false);
            if (old == Visibility.SPECTATOR)
                sendGameModeUpdate(other, getGameMode());
        }

        if (current != Visibility.VISIBLE) {
            sendMetaInvisUpdate(other, true);
            if (current == Visibility.SPECTATOR)
                sendGameModeUpdate(other, GameMode.SPECTATOR);
        }

        other.sendPacket(new BundlePacket());

        visibilityByEntity.put(other.getEntityId(), current.ordinal());
    }

    private void sendMetaInvisUpdate(@NotNull Player player, boolean invisible) {
        byte metaFlags = EntityMetadataStealer.steal(this).getIndex(0, (byte) 0);
        if (invisible) metaFlags |= 0x20; // Ensure the invisible flag is set
        player.sendPacket(new EntityMetaDataPacket(getEntityId(), Map.of(0, Metadata.Byte(metaFlags))));
    }

    private void sendGameModeUpdate(@NotNull Player player, @NotNull GameMode gameMode) {
        var infoEntry = new PlayerInfoUpdatePacket.Entry(
                getUuid(), getUsername(), List.of(), false, 0,
                gameMode, // This is the relevant one, we are only updating the gamemode
                null, null
        );
        player.sendPacket(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, infoEntry));
    }

    @Override
    protected @NotNull PlayerInfoUpdatePacket getAddPlayerToList() {
        return new PlayerInfoUpdatePacket(EnumSet.of(PlayerInfoUpdatePacket.Action.ADD_PLAYER, PlayerInfoUpdatePacket.Action.UPDATE_LISTED), List.of(this.infoEntry()));
    }

    private PlayerInfoUpdatePacket.Entry infoEntry() {
        PlayerSkin skin = getSkin();
        List<PlayerInfoUpdatePacket.Property> prop = skin != null ? List.of(new PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature())) : List.of();
        // Listed is always false. SessionManager manages the tab list for us.
        return new PlayerInfoUpdatePacket.Entry(getUuid(), getUsername(), prop, false, getLatency(), getGameMode(), getDisplayName(), null);
    }

    @Override
    public void spawn() {
        super.spawn();
    }
}
