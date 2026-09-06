package net.hollowcube.compat.axiom;

import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.compat.axiom.data.AxiomPermission;
import net.hollowcube.compat.axiom.events.AxiomEnabledEvent;
import net.hollowcube.compat.axiom.packets.clientbound.*;
import net.hollowcube.compat.axiom.properties.registry.PropertyRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/// One player's Axiom negotiation and editor state for a single backend connection. Desired state (build mode)
/// and actual state (what the client has been told) are kept apart because API 10 clients reply to the hello
/// asynchronously, after their own authorization check.
@NotNullByDefault
public final class AxiomPlayer {

    private static final Tag<AxiomPlayer> TAG = Tag.Transient("axiom:player");
    static final String PROTOCOL_MISMATCH = "Axiom is unavailable until your Minecraft version matches the server.";

    private final Player player;
    private final AxiomTunnel tunnel = new AxiomTunnel();
    private final Set<UUID> ignoredEntities = ConcurrentHashMap.newKeySet();

    private int api = -1;
    private long handshakeId = 0;
    private boolean desired = false;
    private boolean enabled = false;

    private AxiomPlayer(Player player) {
        this.player = player;
    }

    public static AxiomPlayer get(Player player) {
        return player.updateAndGetTag(TAG, existing -> Objects.requireNonNullElseGet(existing, () -> new AxiomPlayer(player)));
    }

    public Player player() {
        return player;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /// The negotiated Axiom API, or -1 until the client has said hello.
    public int api() {
        return api;
    }

    public void setEnabled(boolean enabled) {
        this.desired = enabled;
        if (!enabled) this.tunnel.reset();
        reconcile();
    }

    public void redoHandshake() {
        if (this.enabled) {
            this.enabled = false;
            deactivate();
        }
        beginHandshake();
    }

    public void updateIgnoredEntities(Consumer<Set<UUID>> updater) {
        updater.accept(this.ignoredEntities);
        new AxiomClientboundIgnoreDisplayEntitiesPacket(this.ignoredEntities).send(player);
    }

    AxiomTunnel tunnel() {
        return tunnel;
    }

    void beginHandshake() {
        // A fresh hello makes the client discard its editor state on the next client tick, so forget any prior
        // negotiation and let the client's reply re-enable us. Enabling now (off a stale api from an earlier
        // negotiation on this connection) would send a snapshot the client's handshake-reset then wipes.
        this.api = -1;
        this.enabled = false;
        this.tunnel.reset();

        long id;
        do {
            id = ThreadLocalRandom.current().nextLong();
        } while (id == 0);
        this.handshakeId = id;
        new AxiomClientboundHelloPacket(id).send(player, true);
    }

    void onHello(int clientApi, long clientHandshakeId, boolean protocolCompatible) {
        switch (clientApi) {
            case AxiomAPI.API_9 -> {
                // API 9 clients hello on their own; a later conflicting hello never switches a negotiated API,
                // and protocol mismatches are already reported by the unsupported-mods join warning.
                if (this.api != -1 || !protocolCompatible) return;
                this.handshakeId = 0;
                this.api = AxiomAPI.API_9;
                reconcile();
            }
            case AxiomAPI.API_10 -> {
                if (this.handshakeId == 0 || clientHandshakeId != this.handshakeId) {
                    if (this.api == -1) goodbye("Invalid handshake ID: " + clientHandshakeId);
                    return;
                }
                this.handshakeId = 0;
                if (!protocolCompatible) {
                    goodbye(PROTOCOL_MISMATCH);
                    return;
                }
                this.api = AxiomAPI.API_10;
                reconcile();
            }
            default -> player.sendMessage(clientApi < AxiomAPI.MIN_API_VERSION
                    ? "Incompatible Axiom API version. Please update your mod."
                    : "Axiom API version is too new. Please be patient while we update the server. For now, you can use an older version of the mod.");
        }
    }

    void onInstanceLeave() {
        this.tunnel.reset();
        this.ignoredEntities.clear();
    }

    void onDisconnect() {
        this.tunnel.reset();
        player.removeTag(TAG);
    }

    static boolean isProtocolCompatible(Player player) {
        return ProtocolVersions.getProtocolVersion(player) == MinecraftServer.PROTOCOL_VERSION;
    }

    private void reconcile() {
        if (this.api == -1) return;
        if (this.desired && !this.enabled) {
            this.enabled = true;
            activate();
        } else if (!this.desired && this.enabled) {
            this.enabled = false;
            deactivate();
        }
    }

    private void goodbye(String reason) {
        new AxiomClientboundGoodbyePacket(reason).send(player, true);
    }

    private void activate() {
        var instance = player.getInstance();
        @SuppressWarnings("UnstableApiUsage")
        var dimension = instance.getCachedDimensionType();
        var border = instance.getWorldBorder();
        var min = new BlockVec(border.centerX() - border.diameter() / 2, dimension.minY(), border.centerZ() - border.diameter() / 2);
        var max = new BlockVec(border.centerX() + border.diameter() / 2, dimension.maxY(), border.centerZ() + border.diameter() / 2);

        var restrictions = new AxiomClientboundSetRestrictionsPacket.Restrictions(
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

        // API 10 clients declared their protocol in the handshake, so nothing waits for minecraft:register.
        boolean direct = this.api == AxiomAPI.API_10;
        new AxiomClientboundEnablePacket(serverConfig()).send(player, direct);
        new AxiomClientboundRegisterWorldPropertiesPacket(player, PropertyRegistry.CATEGORIES).send(player, direct);
        new AxiomClientboundSetRestrictionsPacket(restrictions).send(player);
        if (!this.ignoredEntities.isEmpty()) new AxiomClientboundIgnoreDisplayEntitiesPacket(this.ignoredEntities).send(player);

        EventDispatcher.call(new AxiomEnabledEvent(player, true));
    }

    private void deactivate() {
        boolean direct = this.api == AxiomAPI.API_10;
        new AxiomClientboundEnablePacket(null).send(player, direct);
        new AxiomClientboundRegisterWorldPropertiesPacket(player, Map.of()).send(player, direct);

        EventDispatcher.call(new AxiomEnabledEvent(player, false));
    }

    private AxiomClientboundEnablePacket.ServerConfig serverConfig() {
        return switch (this.api) {
            case AxiomAPI.API_9 -> new AxiomClientboundEnablePacket.ServerConfig.V9(
                    AxiomAPI.MAX_BUFFER_SIZE, AxiomAPI.BLUEPRINT_VERSION, List.of(), List.of());
            case AxiomAPI.API_10 -> new AxiomClientboundEnablePacket.ServerConfig.V10(
                    AxiomAPI.BLUEPRINT_VERSION, AxiomAPI.TUNNEL_SPLIT_SIZE, AxiomAPI.MAX_TUNNEL_PACKET_SIZE,
                    List.of(), List.of(), AxiomPackets.ADVERTISED);
            default -> throw new IllegalStateException("Unsupported Axiom API " + this.api);
        };
    }
}
