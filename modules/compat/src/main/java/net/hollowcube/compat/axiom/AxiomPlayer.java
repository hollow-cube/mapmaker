package net.hollowcube.compat.axiom;

import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.axiom.data.AxiomCapabilities;
import net.hollowcube.compat.axiom.events.AxiomEnabledEvent;
import net.hollowcube.compat.axiom.packets.clientbound.*;
import net.hollowcube.compat.axiom.properties.registry.PropertyRegistry;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public final class AxiomPlayer {

    static final Tag<Boolean> AXIOM_ENABLED = Tag.<Boolean>Transient("axiom:enabled").defaultValue(false);
    static final Tag<Integer> AXIOM_VERSION = Tag.<Integer>Transient("axiom:version").defaultValue(-1);
    static final Tag<Set<UUID>> AXIOM_IGNORED_ENTITIES = Tag.<Set<UUID>>Transient("axiom:ignored_entities").defaultValue(HashSet::new);

    private static final AxiomClientboundEnablePacket.ServerConfig SERVER_CONFIG = new AxiomClientboundEnablePacket.ServerConfig(
            0x100000, // 1mb,
            false, false,
            5,
            16, true, // Editor Tabs
            List.of(), List.of(), // Custom Blocks
            AxiomAPI.BLUEPRINT_VERSION
    );

    public static boolean isEnabled(@NotNull Player player) {
        return player.getTag(AXIOM_ENABLED) == Boolean.TRUE;
    }

    public static void setEnabled(@NotNull Player player, boolean enabled) {
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
                    true, true, true, true, // TODO enable annotations
                    AxiomCapabilities.ALL, -1, 0,
                    Pair.of(min, max)
            );

            packets = List.of(
                    new AxiomClientboundEnablePacket(SERVER_CONFIG),
                    new AxiomClientboundRegisterWorldPropertiesPacket(player, PropertyRegistry.CATEGORIES),
                    new AxiomClientboundSetRestrictionsPacket(restrictions),
                    new AxiomClientboundAllowedGamemodesPacket(EnumSet.of(GameMode.CREATIVE, GameMode.SPECTATOR))
            );
        } else {
            packets = List.of(
                    new AxiomClientboundEnablePacket(null),
                    new AxiomClientboundRegisterWorldPropertiesPacket(player, Map.of()),
                    new AxiomClientboundAllowedGamemodesPacket(EnumSet.allOf(GameMode.class))
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
        new AxiomClientboundIgnoreDisplayEntitiesPacket(new ArrayList<>(entities)).send(player);
    }
}
