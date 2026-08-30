package net.hollowcube.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.CookieReceiveEvent;
import com.velocitypowered.api.event.player.CookieStoreEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.configuration.PlayerFinishedConfigurationEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.GameProfile;
import net.hollowcube.ipc.session.SessionClient;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Plugin(id = "hc-proxy", name = "hollowcube proxy plugin", version = "1.0", authors = "hollow cube")
public class ProxyPlugin {
    private static final ChannelIdentifier PROTOCOL_VERSION_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "pvn");
    private static final ChannelIdentifier TRANSFER_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "transfer");
    private static final ChannelIdentifier RESOURCE_PACK_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "resource_pack");
    private static final ChannelIdentifier DISCONNECT_MESSAGE_ID = MinecraftChannelIdentifier.create("velocity", "disconnect");
    private static final Key TRANSFER_DATA_COOKIE = Key.key("mapmaker", "transfer_data");

    private static final Set<ProtocolVersion> SUPPORTED_VERSIONS = Set.of(
        ProtocolVersion.MINECRAFT_1_21_7,
        ProtocolVersion.MINECRAFT_1_21_9,
        ProtocolVersion.MINECRAFT_1_21_11,
        ProtocolVersion.MINECRAFT_26_1,
        ProtocolVersion.MINECRAFT_26_2
    );
    private static final ProtocolVersion RECOMMEND_VERSION = ProtocolVersion.MINECRAFT_26_2;
    private static final String PROTOCOL_VERSION_STRING = "1.21.7-26.2";

    // The port ProxyHttpServer serves the deployment on; 0 (the default) is no http side at all.
    private static final int HTTP_PORT = parsePort(System.getenv("PROXY_HTTP_PORT"));
    // The root the ipc services are served under, the same one every other ipc client is built on.
    private static final String IPC_SERVICE_URL = Objects.requireNonNullElse(System.getenv("IPC_SERVICE_URL"), "http://api-server-java:9124");
    // How long a fetched player count answers pings for before one of them fetches again.
    private static final Duration ONLINE_PLAYERS_TTL = Duration.ofSeconds(2);

    private final Logger logger;
    private final ProxyServer proxy;

    private ProxySessionService sessionService;
    private final SessionClient sessions;

    // What a ping answers with: the whole network's count off the session table, since this proxy
    // only sees its own players. See onlinePlayers().
    private final ReentrantLock onlinePlayersLock = new ReentrantLock();
    private volatile int onlinePlayers;
    private volatile long onlinePlayersExpiry = System.nanoTime();

    private final RegisteredServer anyhubServer;

    private @Nullable ProxyHttpServer http;
    // Set by /drain and never cleared: a draining proxy is one being replaced, and it takes no
    // new logins so it can empty out and be stopped. See ProxyHttpServer.
    private final AtomicBoolean draining = new AtomicBoolean();

    // Map of player uuid to the resource pack hash they currently have applied
    private final Map<UUID, String> resourcePacks = new ConcurrentHashMap<>();
    private final Map<UUID, byte[]> transferData = new ConcurrentHashMap<>();

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

        anyhubServer = proxy.getServer("anyhub").orElseThrow();

        logger.info("hello, world!!!!");
    }

    @Subscribe
    public void handleInitialize(@NotNull ProxyInitializeEvent event) {
        http = ProxyHttpServer.start(logger, HTTP_PORT, this::drain, proxy::getPlayerCount);
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

    @Subscribe
    public void handleShutdown(@NotNull ProxyShutdownEvent event) {
        if (http != null) http.close();
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
        event.setProvider(s -> p -> Tristate.FALSE);
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
        if (!playersJustJoined.contains(playerId)) return;

        playersJustJoined.remove(playerId);
        playerConnectAttempts.remove(playerId);
    }

    @Subscribe
    public void handleDisconnect(@NotNull DisconnectEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        try {
            sessionService.deleteSession(playerId.toString());
        } catch (Exception e) {
            logger.error("failed to delete session (v2) for {}", playerId, e);
        } finally {
            resourcePacks.remove(playerId);
            playersJustJoined.remove(playerId);
            playerConnectAttempts.remove(playerId);
        }
    }

    @Subscribe
    public void handleKickedFromServer(@NotNull KickedFromServerEvent event) {
        if (event.kickedDuringServerConnect()) return;

        // If they were leaving the limbo, they should be disconnected completely no redirect.
        var serverName = event.getServer().getServerInfo().getName();

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
