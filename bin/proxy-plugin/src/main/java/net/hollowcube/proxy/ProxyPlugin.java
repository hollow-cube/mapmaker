package net.hollowcube.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.CookieReceiveEvent;
import com.velocitypowered.api.event.player.CookieStoreEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.configuration.PlayerFinishedConfigurationEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.HandshakeIntent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.GameProfile;
import io.prometheus.client.CollectorRegistry;
import net.hollowcube.anticheat.capture.CaptureClock;
import net.hollowcube.ipc.session.SessionClient;
import net.hollowcube.proxy.anticheat.AnticheatConfig;
import net.hollowcube.proxy.anticheat.AnticheatConnections;
import net.hollowcube.proxy.anticheat.TraceShipper;
import net.hollowcube.proxy.anticheat.VelocityInternals;
import net.hollowcube.proxy.drain.DrainCookie;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import com.velocitypowered.api.util.ModInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Plugin(id = "hc-proxy", name = "hollowcube proxy plugin", version = "1.0", authors = "hollow cube")
public class ProxyPlugin {
    private static final ChannelIdentifier PROTOCOL_VERSION_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "pvn");
    private static final ChannelIdentifier TRANSFER_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "transfer");
    private static final ChannelIdentifier RESOURCE_PACK_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "resource_pack");
    private static final ChannelIdentifier DISCONNECT_MESSAGE_ID = MinecraftChannelIdentifier.create("velocity", "disconnect");
    private static final ChannelIdentifier ANTICHEAT_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "anticheat");
    private static final Key TRANSFER_DATA_COOKIE = Key.key("mapmaker", "transfer_data");
    private static final Key DRAIN_TRANSFER_COOKIE = Key.key("mapmaker", "drain_transfer");

    private static final Set<ProtocolVersion> SUPPORTED_VERSIONS = Set.of(
        ProtocolVersion.MINECRAFT_1_21_7,
        ProtocolVersion.MINECRAFT_1_21_9,
        ProtocolVersion.MINECRAFT_1_21_11,
        ProtocolVersion.MINECRAFT_26_1,
        ProtocolVersion.MINECRAFT_26_2
    );
    private static final ProtocolVersion RECOMMEND_VERSION = ProtocolVersion.MINECRAFT_26_2;
    private static final String PROTOCOL_VERSION_STRING = "1.21.7-26.2";
    // Lands in every capture trace header and store row, so a trace can be tied back to the build
    // that wrote it; the commit hash, stamped into the jar by the writeBuildStamp task.
    private static final String PROXY_VERSION = readBuildStamp();

    // The port ProxyHttpServer serves the deployment on; 0 (the default) is no http side at all.
    private static final int HTTP_PORT = parsePort(System.getenv("PROXY_HTTP_PORT"));
    // The root the ipc services are served under, the same one every other ipc client is built on.
    private static final String IPC_SERVICE_URL = Objects.requireNonNullElse(System.getenv("IPC_SERVICE_URL"), "http://api-server-java:9124");
    // How long a fetched player count answers pings for before one of them fetches again.
    private static final Duration ONLINE_PLAYERS_TTL = Duration.ofSeconds(2);
    // How often the ring gauge and the ring-cap drop counter are read off the connections.
    private static final Duration ANTICHEAT_SAMPLE_INTERVAL = Duration.ofSeconds(10);

    private static final String COOKIE_SECRET_FILE = System.getenv("PROXY_COOKIE_SECRET_FILE");
    private static final Duration DRAIN_COOKIE_TTL = Duration.ofSeconds(30);
    // Bounded because the initial server connect waits on the answer.
    private static final Duration DRAIN_COOKIE_WAIT = Duration.ofSeconds(3);
    // storeCookie and transferToHost each write their packet from their own event continuation, so
    // called back to back they are not ordered. This gap keeps the cookie in front of the transfer.
    private static final Duration DRAIN_TRANSFER_SETTLE = Duration.ofMillis(250);
    private static final Duration DRAIN_PENDING_WINDOW = Duration.ofSeconds(30);
    private static final Duration DRAIN_SWEEP_INTERVAL = Duration.ofSeconds(1);
    private static final Duration HANDSHAKE_INTENT_TTL = Duration.ofSeconds(60);

    private final Logger logger;
    private final ProxyServer proxy;

    private ProxySessionService sessionService;
    private final SessionClient sessions;

    // What a ping answers with: the whole network's count off the session table, since this proxy
    // only sees its own players. See onlinePlayers().
    private final ReentrantLock onlinePlayersLock = new ReentrantLock();
    private volatile int onlinePlayers;
    private volatile long onlinePlayersExpiry = System.nanoTime();

    private final AnticheatConfig anticheatConfig;
    private final AnticheatConnections anticheatConnections;
    private final TraceShipper anticheatShipper;

    private final RegisteredServer anyhubServer;

    private @Nullable ProxyHttpServer http;
    // Set by /drain and never cleared: a draining proxy is one being replaced, and it takes no
    // new logins so it can empty out and be stopped. See ProxyHttpServer.
    private final AtomicBoolean draining = new AtomicBoolean();

    // Map of player uuid to the resource pack hash they currently have applied
    private final Map<UUID, String> resourcePacks = new ConcurrentHashMap<>();
    private final Map<UUID, byte[]> transferData = new ConcurrentHashMap<>();

    private final @Nullable DrainCookie drainCookie;

    // Player to the nanoTime deadline their session row is left alone until. See optimisticTransfer.
    private final Map<UUID, Long> pendingTransfers = new ConcurrentHashMap<>();
    // Keyed by remote address because it is the only thing ConnectionHandshakeEvent and
    // PostLoginEvent share, and an ip:port is exactly one tcp connection.
    private final Map<InetSocketAddress, Long> transferIntents = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<byte[]>> drainCookieWaiters = new ConcurrentHashMap<>();
    private final Map<UUID, String> drainTargets = new ConcurrentHashMap<>();

    private final Set<UUID> playersJustJoined = new CopyOnWriteArraySet<>();
    private final Map<UUID, Integer> playerConnectAttempts = new ConcurrentHashMap<>();

    @Inject
    public ProxyPlugin(@NotNull Logger logger, @NotNull ProxyServer proxy) {
        this.logger = logger;
        this.proxy = proxy;

        var sessionServiceUrl = System.getenv("SESSION_SERVICE_URL");
        if (sessionServiceUrl != null) sessionService = new ProxySessionService(logger, sessionServiceUrl);
        else sessionService = new ProxySessionService(logger, "http://api-server:9124"); // tilt
        sessions = new SessionClient(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
            IPC_SERVICE_URL);

        proxy.getChannelRegistrar().register(TRANSFER_MESSAGE_ID);
        proxy.getChannelRegistrar().register(RESOURCE_PACK_MESSAGE_ID);
        proxy.getChannelRegistrar().register(PROTOCOL_VERSION_MESSAGE_ID);
        proxy.getChannelRegistrar().register(DISCONNECT_MESSAGE_ID);
        proxy.getChannelRegistrar().register(ANTICHEAT_MESSAGE_ID);

        anyhubServer = proxy.getServer("anyhub").orElseThrow();
        drainCookie = DrainCookie.load(logger, COOKIE_SECRET_FILE);

        anticheatConfig = AnticheatConfig.fromEnv(logger);
        anticheatShipper = new TraceShipper(IPC_SERVICE_URL, anticheatConfig);
        // Velocity disconnects every player before it fires ProxyShutdownEvent, so a capture open
        // at shutdown is closed by the channel going inactive; this is what tells that apart from
        // the player leaving on their own.
        anticheatConnections = new AnticheatConnections(anticheatConfig, CaptureClock.SYSTEM,
            ProxySessionService.hostname, PROXY_VERSION, () -> VelocityInternals.isShuttingDown(proxy),
            anticheatShipper);

        logger.info("hello, world!!!!");
    }

    @Subscribe
    public void handleInitialize(@NotNull ProxyInitializeEvent event) {
        http = ProxyHttpServer.start(logger, HTTP_PORT, this::drain,
            () -> new ProxyHttpServer.Drain(proxy.getPlayerCount(), pendingTransfers.size()),
            CollectorRegistry.defaultRegistry);
        proxy.getScheduler().buildTask(this, this::sweepDrainState)
            .repeat(DRAIN_SWEEP_INTERVAL)
            .schedule();

        logger.info("anticheat: {}", anticheatConfig);
        if (!anticheatConfig.enabled()) return;
        // Also picks up whatever the last run of this proxy left on the spool volume.
        anticheatShipper.start();
        proxy.getScheduler().buildTask(this, anticheatConnections::sample)
            .repeat(ANTICHEAT_SAMPLE_INTERVAL)
            .schedule();
    }

    /// Fetched by the ping that finds it expired and held for [#ONLINE_PLAYERS_TTL]: a ping can
    /// afford the call, and a timer would be one per proxy for nobody looking. Pings arriving
    /// while another is fetching answer with the count that stands, as do the ones after a failed
    /// fetch until the ttl passes again, since a stale number beats a zero.
    private int onlinePlayers() {
        if (System.nanoTime() - onlinePlayersExpiry < 0 || !onlinePlayersLock.tryLock()) return onlinePlayers;
        try {
            if (System.nanoTime() - onlinePlayersExpiry < 0) return onlinePlayers;
            try {
                onlinePlayers = sessions.onlinePlayers();
            } catch (Exception e) {
                logger.warn("failed to read the online player count: {}", e.toString());
            }
            onlinePlayersExpiry = System.nanoTime() + ONLINE_PLAYERS_TTL.toNanos();
            return onlinePlayers;
        } finally {
            onlinePlayersLock.unlock();
        }
    }

    /// Every open capture is closed as `closedBy=shutdown` and given the configured grace to be
    /// assembled and reach the store; whatever is still in flight when it runs out stays on the
    /// spool volume for the next process to sweep up.
    @Subscribe
    public void handleShutdown(@NotNull ProxyShutdownEvent event) {
        var deadline = System.nanoTime() + anticheatConfig.shutdownGrace().toNanos();
        if (http != null) http.close();
        anticheatConnections.close();
        anticheatShipper.close(Duration.ofNanos(Math.max(0, deadline - System.nanoTime())));
    }

    @Subscribe
    public void handleHandshake(@NotNull ConnectionHandshakeEvent event) {
        if (event.getIntent() != HandshakeIntent.TRANSFER) return;
        transferIntents.put(event.getConnection().getRemoteAddress(), System.nanoTime() + HANDSHAKE_INTENT_TTL.toNanos());
    }

    /// The first event with a channel behind the player and a settled protocol version — both
    /// directions are in the configuration phase — which is why the anticheat tap goes in here.
    /// Velocity also awaits this event before choosing an initial server, so a cookie round trip
    /// started here is finished by the time [#handleChooseInitialServer] needs it.
    @Subscribe
    public @Nullable EventTask handlePostLogin(@NotNull PostLoginEvent event) {
        var player = event.getPlayer();
        try {
            anticheatConnections.join(player, player.getUniqueId(), player.getUsername(),
                player.getProtocolVersion().getProtocol(), player::getClientBrand, anticheatExtras(player));
        } catch (Exception e) {
            logger.error("failed to install the anticheat tap for {}", player.getUsername(), e);
        }

        if (drainCookie == null) return null;
        if (transferIntents.remove(player.getRemoteAddress()) == null) return null;

        var playerId = player.getUniqueId();
        var answer = new CompletableFuture<byte[]>();
        drainCookieWaiters.put(playerId, answer);
        try {
            player.requestCookie(DRAIN_TRANSFER_COOKIE);
        } catch (IllegalArgumentException e) {
            drainCookieWaiters.remove(playerId);
            return null; // Pre-1.20.5, so it cannot have been transferred here in the first place.
        }
        return EventTask.resumeWhenComplete(answer
            .orTimeout(DRAIN_COOKIE_WAIT.toMillis(), TimeUnit.MILLISECONDS)
            .handle((data, error) -> {
                drainCookieWaiters.remove(playerId);
                openDrainCookie(player, error == null ? data : null);
                return null;
            }));
    }

    /// The forge mod list the client hands over at login, when there is one: the only mod list the
    /// proxy ever sees, and a header extra so a trace says which mods were behind its movement.
    private static Map<String, String> anticheatExtras(Player player) {
        var mods = player.getModInfo().map(ModInfo::getMods).orElse(List.of());
        if (mods.isEmpty()) return Map.of();
        var names = new ArrayList<String>(mods.size());
        for (var mod : mods) names.add(mod.getId() + ":" + mod.getVersion());
        Collections.sort(names);
        return Map.of("mods", String.join(",", names));
    }

    private void openDrainCookie(@NotNull Player player, byte @Nullable [] data) {
        if (drainCookie == null) return;
        var playerId = player.getUniqueId();
        var transfer = drainCookie.open(playerId, Instant.now(), data);
        if (transfer == null) return;

        logger.info("drain: {} arrived carrying a transfer to {}", player.getUsername(), transfer.address());
        drainTargets.put(playerId, transfer.address());
        if (transfer.transferData().length > 0) transferData.put(playerId, transfer.transferData());
        player.storeCookie(DRAIN_TRANSFER_COOKIE, new byte[0]);
    }

    @Subscribe
    public void handleChooseInitialServer(@NotNull PlayerChooseInitialServerEvent event) {
        var target = drainTargets.remove(event.getPlayer().getUniqueId());
        if (target == null) return;
        var si = new ServerInfo("map-server", new InetSocketAddress(target, 25565));
        event.setInitialServer(proxy.createRawRegisteredServer(si));
    }

    private void drain() {
        if (draining.compareAndSet(false, true))
            logger.info("draining: no new logins, {} players still connected", proxy.getPlayerCount());
    }

    /// Before mojang auth, so a login a draining proxy turns away costs it nothing. The player
    /// reconnects and lands on whichever proxy is ready, which is the one replacing this.
    @Subscribe
    public void handlePreLogin(@NotNull PreLoginEvent event) {
        if (!draining.get()) return;
        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
            Component.text("This proxy is restarting, please reconnect.")));
    }

    @Subscribe
    public void handlePermissionSetup(@NotNull PermissionsSetupEvent event) {
        // Always deny all permissions
        event.setProvider(_ -> _ -> Tristate.FALSE);
    }

    @Subscribe
    public void handleLogin(@NotNull LoginEvent event) {
        var player = event.getPlayer();

        try {
            String skinTexture = null, skinSignature = null;
            var texProp = getGPProperty(player.getGameProfile(), "textures");
            if (texProp != null) {
                skinTexture = texProp.getValue();
                skinSignature = texProp.getSignature();
            }

            var protocolVersion = player.getProtocolVersion();
            var pd = sessionService.createSession(
                player.getUniqueId().toString(),
                new SessionCreateRequest(
                    ProxySessionService.hostname,
                    player.getUsername(),
                    player.getRemoteAddress().getAddress().getHostAddress(),
                    new SessionCreateRequest.Skin(skinTexture, skinSignature),
                    player.getRawVirtualHost().orElse(null),
                    protocolVersion.getProtocol(), protocolVersion.getMostRecentSupportedVersion()
                )
            );
            playersJustJoined.add(player.getUniqueId());
            logger.info("created session (v2) for {}: {}", player.getUsername(), pd);
        } catch (ProxySessionService.SessionCreationDeniedError error) {
            event.setResult(ResultedEvent.ComponentResult.denied(error.reason()));
        } catch (Exception e) {
            logger.error("failed to create session (v2) for {}", player.getUsername(), e);
            event.setResult(LoginEvent.ComponentResult.denied(Component.text("failed to create session")));
        }
    }

    @Subscribe
    public void handleStatusMessage(@NotNull ProxyPingEvent event) {
        var builder = event.getPing().asBuilder();
        var version = event.getConnection().getProtocolVersion();
        var protocol = SUPPORTED_VERSIONS.contains(version) ? version : RECOMMEND_VERSION;

        builder.version(new ServerPing.Version(protocol.getProtocol(), PROTOCOL_VERSION_STRING));
        builder.onlinePlayers(onlinePlayers());
        event.setPing(builder.build());
    }

    @Subscribe
    public void handlePluginMessage(@NotNull PluginMessageEvent event) {
        logger.info("plugin message: {}", event.getIdentifier());
        if (TRANSFER_MESSAGE_ID.equals(event.getIdentifier())) {
            handleTransfer(event);
        } else if (RESOURCE_PACK_MESSAGE_ID.equals(event.getIdentifier())) {
            handleResourcePack(event);
        } else if (PROTOCOL_VERSION_MESSAGE_ID.equals(event.getIdentifier())) {
            handleProtocolVersionRequest(event);
        } else if (DISCONNECT_MESSAGE_ID.equals(event.getIdentifier())) {
            handleDisconnectMessage(event);
        } else if (ANTICHEAT_MESSAGE_ID.equals(event.getIdentifier())) {
            handleAnticheatControl(event);
        }
    }

    @Subscribe
    public void handleCookieStore(@NotNull CookieStoreEvent event) {
        if (!event.getOriginalKey().equals(TRANSFER_DATA_COOKIE))
            return;

        event.setResult(CookieStoreEvent.ForwardResult.handled()); // Never forward
        transferData.put(event.getPlayer().getUniqueId(), event.getOriginalData());
    }

    // We reply on the RECEIVE event, ie replacing the client saying they don't have the cookie.
    // This should really be done on the CookieRequestEvent, but velocity is brain-damaged and doesn't let
    // you reply to the cookie in that event. You have to let the cookie go to the client and have them
    // reply, thus exposing a detail about what we do and making a useless req/res down to the client.
    // AWESOME JOB GUYS YOU ARE DOING GREAT!!!
    @Subscribe
    public void handleCookieResponse(@NotNull CookieReceiveEvent event) {
        if (event.getOriginalKey().equals(DRAIN_TRANSFER_COOKIE)) {
            event.setResult(CookieReceiveEvent.ForwardResult.handled());
            var waiter = drainCookieWaiters.remove(event.getPlayer().getUniqueId());
            if (waiter != null) waiter.complete(event.getOriginalData());
            return;
        }

        if (!event.getOriginalKey().equals(TRANSFER_DATA_COOKIE))
            return;

        var data = transferData.get(event.getPlayer().getUniqueId());
        event.setResult(CookieReceiveEvent.ForwardResult.data(data));
    }

    @Subscribe
    public void handleConfigEnd(@NotNull PlayerFinishedConfigurationEvent event) {
        transferData.remove(event.player().getUniqueId());
    }

//    @Subscribe
//    public void handleConfigStart(@NotNull PlayerEnteredConfigurationEvent event) {
//        event.player().transferToHost(new InetSocketAddress("ovh-02.hollowcube.dev", 30565));
//    }

    /// Capture control, from the backend the player is on. Never forwarded either way: what the
    /// backend sends is for the proxy, and anything a client puts on this channel is dropped
    /// without comment — the backend is the only thing that opens a capture.
    private void handleAnticheatControl(@NotNull PluginMessageEvent event) {
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (event.getSource() instanceof ServerConnection serverConn)
            anticheatConnections.handleBackend(serverConn.getPlayer().getUniqueId(),
                serverConn.getPlayer().getUsername(), event.getData());
    }

    private void handleDisconnectMessage(@NotNull PluginMessageEvent event) {
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection serverConn)) return;
        var player = serverConn.getPlayer();

        var reason = new String(event.getData(), StandardCharsets.UTF_8);
        player.disconnect(GsonComponentSerializer.gson().deserialize(reason));
    }

    private void handleResourcePack(@NotNull PluginMessageEvent event) {
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection serverConn)) return;
        var player = serverConn.getPlayer();

        var newResourcePack = new String(event.getData());
        var existingResourcePack = resourcePacks.get(player.getUniqueId());

        var shouldSend = existingResourcePack == null || !existingResourcePack.equals(newResourcePack);
        serverConn.sendPluginMessage(RESOURCE_PACK_MESSAGE_ID, String.valueOf(shouldSend).getBytes(StandardCharsets.UTF_8));
        if (shouldSend) {
            resourcePacks.put(player.getUniqueId(), newResourcePack);
        }
    }

    private void handleTransfer(@NotNull PluginMessageEvent event) {
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection serverConn)) return;
        var player = serverConn.getPlayer();

        var serverName = new String(event.getData());
        logger.info("transfering {} to {}", player.getUsername(), serverName);

        if (draining.get() && optimisticTransfer(player, serverName)) return;

        var si = new ServerInfo("map-server", new InetSocketAddress(serverName, 25565));
        player.createConnectionRequest(proxy.createRawRegisteredServer(si)).connect().thenAccept(result -> {
            switch (result.getStatus()) {
                case SUCCESS, ALREADY_CONNECTED ->
                    logger.info("transfer success: {} -> {}", player.getUsername(), serverName);
                case SERVER_DISCONNECTED, CONNECTION_CANCELLED -> {
                    logger.info("transfer failed: {} -> {}", player.getUsername(), serverName);
                    serverConn.sendPluginMessage(TRANSFER_MESSAGE_ID, "fail".getBytes(StandardCharsets.UTF_8));
                }
            }
        });
    }

    /// A backend switch on a draining proxy, answered by moving the player off this proxy instead
    /// of connecting them to the backend from it: they are sent back to the address they arrived
    /// on, which now resolves to the proxy replacing this one, carrying the transfer in a cookie
    /// for that proxy to finish. One reconnect rather than two, and this pod stops being pinned
    /// open by a player who never logs off.
    ///
    /// False when the transfer has to happen the ordinary way, and the caller falls through to it.
    private boolean optimisticTransfer(@NotNull Player player, @NotNull String address) {
        if (drainCookie == null) return false;
        // Vanilla puts the srv-resolved target in the handshake, so this is directly connectable.
        var host = player.getVirtualHost().orElse(null);
        if (host == null) return false;
        if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_5) < 0) return false;

        var playerId = player.getUniqueId();
        var pendingData = transferData.getOrDefault(playerId, new byte[0]);
        var cookie = drainCookie.seal(playerId, Instant.now().plus(DRAIN_COOKIE_TTL), address, pendingData);
        if (cookie.length > DrainCookie.MAX_COOKIE_BYTES) {
            logger.warn("drain: {} bytes of transfer data will not fit a cookie, transferring {} in place",
                pendingData.length, player.getUsername());
            return false;
        }

        transferData.remove(playerId);
        pendingTransfers.put(playerId, System.nanoTime() + DRAIN_PENDING_WINDOW.toNanos());

        player.storeCookie(DRAIN_TRANSFER_COOKIE, cookie);
        proxy.getScheduler().buildTask(this, () -> player.transferToHost(host))
            .delay(DRAIN_TRANSFER_SETTLE)
            .schedule();
        logger.info("drain: transferring {} off this proxy, to {} via {}", player.getUsername(), address, host);
        return true;
    }

    private void handleProtocolVersionRequest(@NotNull PluginMessageEvent event) {
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection serverConn)) return;
        var player = serverConn.getPlayer();

        logger.info("protocol version request from {}", player.getUsername());

        int pvn = player.getProtocolVersion().getProtocol();
        var reply = String.valueOf(pvn).getBytes(StandardCharsets.UTF_8);
        serverConn.sendPluginMessage(PROTOCOL_VERSION_MESSAGE_ID, reply);
    }

    @Subscribe
    public void handlePostConnect(@NotNull ServerPostConnectEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        // A previous server means a switch, which ends the run whatever capture is open was opened
        // for. Null is the first connect of the session, where there is nothing to close.
        if (event.getPreviousServer() != null) anticheatConnections.switchedServer(playerId);

        if (!playersJustJoined.contains(playerId)) return;

        playersJustJoined.remove(playerId);
        playerConnectAttempts.remove(playerId);
    }

    @Subscribe
    public void handleDisconnect(@NotNull DisconnectEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        anticheatConnections.quit(playerId);
        // Deleting the session of a player we transferred off would kick them off the backend they
        // are about to reach, so it is left to sweepDrainState.
        if (!pendingTransfers.containsKey(playerId)) deleteSession(playerId);
        resourcePacks.remove(playerId);
        playersJustJoined.remove(playerId);
        playerConnectAttempts.remove(playerId);
        drainCookieWaiters.remove(playerId);
        drainTargets.remove(playerId);
    }

    /// Fenced on this proxy: the row may belong to another one by now, and every backend kicks the
    /// player it names when a session is deleted.
    private void deleteSession(@NotNull UUID playerId) {
        try {
            sessionService.deleteSession(playerId.toString(), ProxySessionService.hostname);
        } catch (Exception e) {
            logger.error("failed to delete session (v2) for {}", playerId, e);
        }
    }

    /// A transferred player who never turned up anywhere else gets their session row released here,
    /// which is also what the drain is waiting on.
    private void sweepDrainState() {
        long now = System.nanoTime();
        for (var entry : pendingTransfers.entrySet()) {
            if (now - entry.getValue() < 0) continue;
            if (!pendingTransfers.remove(entry.getKey(), entry.getValue())) continue;
            // Still here, so the transfer never took and their session is live and theirs.
            if (proxy.getPlayer(entry.getKey()).isPresent()) {
                logger.warn("drain: {} is still on this proxy a window after being transferred off", entry.getKey());
                continue;
            }
            logger.info("drain: transfer of {} never settled, releasing their session", entry.getKey());
            deleteSession(entry.getKey());
        }
        transferIntents.values().removeIf(deadline -> now - deadline >= 0);
    }

    @Subscribe
    public void handleKickedFromServer(@NotNull KickedFromServerEvent event) {
        var serverName = event.getServer().getServerInfo().getName();

        // If they were leaving the limbo, they should be disconnected completely no redirect.
        if (event.kickedDuringServerConnect()) {
            // A drain cookie sent them straight at a backend that is not there any more, and they
            // have no server to be put back on.
            if (event.getPlayer().getCurrentServer().isEmpty() && !"anyhub".equals(serverName)) {
                logger.info("drain: {} could not reach {}, sending them to the hub",
                    event.getPlayer().getUsername(), serverName);
                event.setResult(KickedFromServerEvent.RedirectPlayer.create(anyhubServer, Component.empty()));
            }
            return;
        }

        // 'anyhub' points to the clusterip service for all the hub instances, so if you are kicked from it
        // velocity assumes it cannot immediately reconnect to it. In reality, reconnecting will point to another
        // ready instance, so it is totally safe to do so.
        if ("anyhub".equals(serverName)) {
            int attempts = playerConnectAttempts.merge(event.getPlayer().getUniqueId(), 1, Integer::sum);
            if (attempts > 5) {
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("Unable to recover. Please try again")));
                return;
            }

            logger.info("reconnecting {} to hub", event.getPlayer().getUsername());
            event.setResult(KickedFromServerEvent.RedirectPlayer.create(anyhubServer, Component.empty()));
        }

    }

    private static String readBuildStamp() {
        try (var in = ProxyPlugin.class.getResourceAsStream("/hc-proxy-build")) {
            if (in == null) return "dev";
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException _) {
            // A jar that cannot read its own resource still comes up; the header just says dev.
            return "dev";
        }
    }

    private static int parsePort(@Nullable String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            int port = Integer.parseInt(value.trim());
            if (port >= 0 && port <= 65535) return port;
        } catch (NumberFormatException ignored) {
        }
        // Logged from the constructor would be nicer, but a static is read before there is one.
        System.err.println("PROXY_HTTP_PORT is not a port: " + value + ", http side disabled");
        return 0;
    }

    private @Nullable GameProfile.Property getGPProperty(@NotNull GameProfile gp, @NotNull String name) {
        return gp.getProperties().stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

}
