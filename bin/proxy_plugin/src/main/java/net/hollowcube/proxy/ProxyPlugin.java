package net.hollowcube.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.GameProfile;
import net.hollowcube.mapmaker.player.PlayerSkin;
import net.hollowcube.mapmaker.player.SessionCreateRequestV2;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.player.SessionServiceImpl;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Plugin(id = "hc-proxy", name = "hollowcube proxy plugin", version = "1.0", authors = "hollow cube")
public class ProxyPlugin {
    private static final ChannelIdentifier TRANSFER_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "transfer");
    private static final ChannelIdentifier TRANSFER_OTHER_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "transfer_other");
    private static final ChannelIdentifier RESOURCE_PACK_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "resource_pack");
    private static final ChannelIdentifier JOIN_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "first_join");

    private final Logger logger;
    private final ProxyServer proxy;

    private SessionService sessionService;

    private final RegisteredServer limboServer;
    private final RegisteredServer anyhubServer;

    // Map of player uuid to the resource pack hash they currently have applied
    private final Map<UUID, String> resourcePacks = new ConcurrentHashMap<>();

    private final Set<UUID> playersWithSession = new CopyOnWriteArraySet<>();
    private final Set<UUID> playersJustJoined = new CopyOnWriteArraySet<>();

    @Inject
    public ProxyPlugin(@NotNull Logger logger, @NotNull ProxyServer proxy) {
        this.logger = logger;
        this.proxy = proxy;

        var sessionServiceUrl = System.getenv("SESSION_SERVICE_URL");
        if (sessionServiceUrl != null) sessionService = new SessionServiceImpl(sessionServiceUrl);
        else sessionService = new SessionServiceImpl("http://session-service:9124"); // tilt

        proxy.getChannelRegistrar().register(TRANSFER_MESSAGE_ID);
        proxy.getChannelRegistrar().register(TRANSFER_OTHER_MESSAGE_ID);
        proxy.getChannelRegistrar().register(RESOURCE_PACK_MESSAGE_ID);

        limboServer = proxy.getServer("limbo").orElse(null);
        anyhubServer = proxy.getServer("anyhub").orElseThrow();

        logger.info("hello, world!!!!");
    }

    @Subscribe
    public void handlePermissionSetup(@NotNull PermissionsSetupEvent event) {
        // Always deny all permissions
        event.setProvider(s -> p -> Tristate.FALSE);
    }

    @Subscribe
    public void handleChooseInitialServer(@NotNull PlayerChooseInitialServerEvent event) {
        if (limboServer != null && !playersWithSession.contains(event.getPlayer().getUniqueId())) {
            logger.info("sending {} to limbo", event.getPlayer().getUsername());
            event.setInitialServer(limboServer);
        }
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

            var pd = sessionService.createSessionV2(
                    player.getUniqueId().toString(),
                    new SessionCreateRequestV2(
                            AbstractHttpService.hostname,
                            player.getUsername(),
                            player.getRemoteAddress().getAddress().getHostAddress(),
                            new PlayerSkin(skinTexture, skinSignature)
                    )
            );
            playersWithSession.add(player.getUniqueId());
            playersJustJoined.add(player.getUniqueId());
            logger.info("created session (v2) for {}: {}", player.getUsername(), pd);
        } catch (SessionService.UnauthorizedError ignored) {
            // this is ok, they will be sent to the limbo
            logger.info("player {} is not in the beta", player.getUsername());
        } catch (Exception e) {
            logger.error("failed to create session (v2) for {}", player.getUsername(), e);
            event.setResult(LoginEvent.ComponentResult.denied(Component.text("failed to create session")));
        }
    }

    @Subscribe
    public void handlePluginMessage(@NotNull PluginMessageEvent event) {
        logger.info("plugin message: {}", event.getIdentifier());
        if (TRANSFER_MESSAGE_ID.equals(event.getIdentifier())) {
            handleTransfer(event);
        } else if (TRANSFER_OTHER_MESSAGE_ID.equals(event.getIdentifier())) {
            handleTransferOther(event);
        } else if (RESOURCE_PACK_MESSAGE_ID.equals(event.getIdentifier())) {
            handleResourcePack(event);
        }
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

    private void handleTransferOther(@NotNull PluginMessageEvent event) {
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection serverConnection)) return;

        var data = AbstractHttpService.GSON.fromJson(new String(event.getData()), TransferOtherRequest.class);

        var targetPlayerId = data.targetPlayerId();
        var targetPlayer = proxy.getPlayer(targetPlayerId).orElse(null);
        if (targetPlayer == null) {
            logger.error("transfer_other target player not found: {}", targetPlayerId);
            return;
        }

        var serverName = data.serverName();
        logger.info("transferring {} to {}", targetPlayerId, serverName);

        var info = new ServerInfo("map-server", new InetSocketAddress(serverName, 25565));
        targetPlayer.createConnectionRequest(proxy.createRawRegisteredServer(info)).connect().thenAccept(result -> {
            switch (result.getStatus()) {
                case SUCCESS, ALREADY_CONNECTED ->
                        logger.info("transfer_other success: {} -> {}", targetPlayerId, serverName);
                case SERVER_DISCONNECTED, CONNECTION_CANCELLED -> {
                    logger.info("transfer_other failed: {} -> {}", targetPlayerId, serverName);
                    serverConnection.sendPluginMessage(TRANSFER_OTHER_MESSAGE_ID, "fail".getBytes(StandardCharsets.UTF_8));
                }
            }
        });
    }

    private record TransferOtherRequest(@NotNull UUID targetPlayerId, @NotNull String serverName) {
    }

    @Subscribe
    public void handlePostConnect(@NotNull ServerPostConnectEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        if (!playersJustJoined.contains(playerId)) return;

        playersJustJoined.remove(playerId);
        event.getPlayer().getCurrentServer().ifPresent(ignored ->
                event.getPlayer().sendPluginMessage(JOIN_MESSAGE_ID, new byte[0]));
    }

    @Subscribe
    public void handleDisconnect(@NotNull DisconnectEvent event) {
        if (!playersWithSession.contains(event.getPlayer().getUniqueId())) return;

        var playerId = event.getPlayer().getUniqueId().toString();
        try {
            sessionService.deleteSessionV2(playerId);
            logger.info("deleted session (v2) for {}", playerId);
        } catch (Exception e) {
            logger.error("failed to delete session (v2) for {}", playerId, e);
        } finally {
            resourcePacks.remove(event.getPlayer().getUniqueId());
            playersWithSession.remove(event.getPlayer().getUniqueId());
            playersJustJoined.remove(event.getPlayer().getUniqueId());
        }
    }

//    @Subscribe
//    public void handleInitialServer(PlayerChooseInitialServerEvent event) {
//        var rs = proxy.createRawRegisteredServer(new ServerInfo("hub-minecraft", new InetSocketAddress("hub-minecraft", 25565)));
//        event.setInitialServer(rs);
//    }

    @Subscribe
    public void handleKickedFromServer(@NotNull KickedFromServerEvent event) {
        if (event.kickedDuringServerConnect()) return;

        // If they were leaving the limbo, they should be disconnected completely no redirect.
        var serverName = event.getServer().getServerInfo().getName();
        if ("limbo".equals(serverName)) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(event.getServerKickReason().orElse(Component.empty())));
            return;
        }

        // 'anyhub' points to the clusterip service for all the hub instances, so if you are kicked from it
        // velocity assumes it cannot immediately reconnect to it. In reality, reconnecting will point to another
        // ready instance, so it is totally safe to do so.
        if ("anyhub".equals(serverName)) {
            logger.info("reconnecting {} to hub", event.getPlayer().getUsername());
            event.setResult(KickedFromServerEvent.RedirectPlayer.create(anyhubServer, Component.empty()));
            return;
        }

//        if (event.getServer().getServerInfo().getName().equals("hub-minecraft")) return;
//
//        var playerId = event.getPlayer().getUniqueId().toString();
//        logger.info("kicked from server, joining a new hub: {}", playerId);

//        var res = sessionService.joinHubV2(new JoinHubRequest(playerId));
//        logger.info("join hub result: {}", res);
//        var rs = proxy.createRawRegisteredServer(new ServerInfo("hub", new InetSocketAddress(res.serverClusterIp(), 25565)));
        //todo in the future we will need to let ss choose a hub to match you with friends, but for now it seems fine to just direct to any hub server?

//        var rs = proxy.createRawRegisteredServer(new ServerInfo("hub-minecraft", new InetSocketAddress("hub-minecraft", 25565)));
//        event.setResult(KickedFromServerEvent.RedirectPlayer.create(rs));
    }

    private @Nullable GameProfile.Property getGPProperty(@NotNull GameProfile gp, @NotNull String name) {
        return gp.getProperties().stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

}
