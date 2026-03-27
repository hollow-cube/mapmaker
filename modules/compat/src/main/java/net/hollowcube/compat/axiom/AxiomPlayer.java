package net.hollowcube.compat.axiom;

import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.axiom.data.AxiomPermission;
import net.hollowcube.compat.axiom.events.AxiomEnabledEvent;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundEnablePacket;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundIgnoreDisplayEntitiesPacket;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundRegisterWorldPropertiesPacket;
import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundSetRestrictionsPacket;
import net.hollowcube.compat.axiom.properties.registry.PropertyRegistry;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class AxiomPlayer {

    static final Tag<Boolean> AXIOM_ENABLED = Tag.<Boolean>Transient("axiom:enabled").defaultValue(false);
    static final Tag<Integer> AXIOM_VERSION = Tag.<Integer>Transient("axiom:version").defaultValue(-1);
    static final Tag<Set<UUID>> AXIOM_IGNORED_ENTITIES = Tag.<Set<UUID>>Transient("axiom:ignored_entities").defaultValue(ConcurrentHashMap::newKeySet);
    static final Tag<Boolean> AXIOM_PENDING_ENABLE = Tag.Transient("axiom:pending_enable");

    private static final AxiomClientboundEnablePacket.ServerConfig SERVER_CONFIG = new AxiomClientboundEnablePacket.ServerConfig(
            0x100000, // 1mb,
            AxiomAPI.BLUEPRINT_VERSION,
            List.of(), List.of() // Custom Blocks
    );

    public static boolean isEnabled(@NotNull Player player) {
        return player.getTag(AXIOM_ENABLED) == Boolean.TRUE;
    }

    public static void setEnabled(@NotNull Player player, boolean enabled) {
        if (getVersion(player) == -1) {
            player.setTag(AXIOM_PENDING_ENABLE, enabled);
        } else {
            enableInternal(player, enabled);
        }
    }

    public static void handlePendingEnable(@NotNull Player player) {
        Boolean enable = player.getAndSetTag(AXIOM_PENDING_ENABLE, null);
        if (enable == null) return;

        int version = getVersion(player);
        if (version >= AxiomAPI.MIN_API_VERSION && version <= AxiomAPI.MAX_API_VERSION) {
            enableInternal(player, enable);
        }
    }

    private static void enableInternal(Player player, boolean enabled) {
        player.setTag(AXIOM_ENABLED, enabled);

        List<ClientboundModPacket<?>> packets;

        if (enabled) {
            var instance = player.getInstance();
            @SuppressWarnings("UnstableApiUsage")
            var dimension = instance.getCachedDimensionType();
            var border = instance.getWorldBorder();
            var min = new BlockVec(border.centerX() - border.diameter() / 2, dimension.minY(), border.centerZ() - border.diameter() / 2);
            var max = new BlockVec(border.centerX() + border.diameter() / 2, dimension.maxY(), border.centerZ() + border.diameter() / 2);

            AxiomClientboundSetRestrictionsPacket.Restrictions restrictions = new AxiomClientboundSetRestrictionsPacket.Restrictions(
                    Set.of(
                            AxiomPermission.ENTITY,
                            AxiomPermission.ANNOTATION,
                            AxiomPermission.DEFAULT
                    ),
                    Set.of(
                            AxiomPermission.PLAYER_GAMEMODE_ADVENTURE,
                            AxiomPermission.PLAYER_GAMEMODE_SURVIVAL
                    ),
                    -1,
                    Pair.of(min, max)
            );

            packets = List.of(
                    new AxiomClientboundEnablePacket(SERVER_CONFIG),
                    new AxiomClientboundRegisterWorldPropertiesPacket(player, PropertyRegistry.CATEGORIES),
                    new AxiomClientboundSetRestrictionsPacket(restrictions)
            );
        } else {
            packets = List.of(
                    new AxiomClientboundEnablePacket(null),
                    new AxiomClientboundRegisterWorldPropertiesPacket(player, Map.of())
            );
        }

        packets.forEach(packet -> packet.send(player));

        EventDispatcher.call(new AxiomEnabledEvent(player, enabled));
    }

    public static int getVersion(@NotNull Player player) {
        return player.getTag(AXIOM_VERSION);
    }

    public static void setVersion(@NotNull Player player, int version) {
        player.setTag(AXIOM_VERSION, version);
    }

    public static void updateIgnoredEntities(@NotNull Player player, @NotNull Consumer<Set<UUID>> updater) {
        var entities = player.updateAndGetTag(AXIOM_IGNORED_ENTITIES, it -> {
            updater.accept(it);
            return it;
        });
        new AxiomClientboundIgnoreDisplayEntitiesPacket(entities).send(player);
    }
}
