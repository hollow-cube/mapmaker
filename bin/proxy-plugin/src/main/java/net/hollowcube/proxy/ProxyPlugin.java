package net.hollowcube.proxy;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.*;
import com.velocitypowered.api.event.player.configuration.PlayerFinishedConfigurationEvent;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Plugin(id = "hc-proxy", name = "hollowcube proxy plugin", version = "1.0", authors = "hollow cube")
public class ProxyPlugin {
    private static final ChannelIdentifier TRANSFER_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "transfer");
    private static final ChannelIdentifier RESOURCE_PACK_MESSAGE_ID = MinecraftChannelIdentifier.create("mapmaker", "resource_pack");
    private static final Key TRANSFER_DATA_COOKIE = Key.key("mapmaker", "transfer_data");

    public static final TextColor RED = TextColor.color(0xFA4141);
    public static final Component MAINTENANCE = Component.text()
            .append(Component.text("The server is currently in maintenance!", RED, TextDecoration.BOLD))
            .appendNewline().appendNewline()
            .append(Component.text("Join the discord for updates!"))
            .appendNewline()
            .append(Component.text("discord.hollowcube.net", TextColor.color(0x3895FF)))
            .build();

    private final Logger logger;
    private final ProxyServer proxy;

    private ProxySessionService sessionService;

    private final RegisteredServer anyhubServer;

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
        else sessionService = new ProxySessionService(logger, "http://session-service:9124"); // tilt

        proxy.getChannelRegistrar().register(TRANSFER_MESSAGE_ID);
        proxy.getChannelRegistrar().register(RESOURCE_PACK_MESSAGE_ID);

        anyhubServer = proxy.getServer("anyhub").orElseThrow();

        Translations.init();
        logger.info("hello, world!!!!");
    }

    @Subscribe
    public void handlePermissionSetup(@NotNull PermissionsSetupEvent event) {
        // Always deny all permissions
        event.setProvider(s -> p -> Tristate.FALSE);
    }

    @Subscribe
    public void handleChooseInitialServer(@NotNull PlayerChooseInitialServerEvent event) {
//        if (!playersWithSession.contains(event.getPlayer().getUniqueId())) {
//            event.getPlayer().disconnect(Component.text("something went wrong"));
//        }
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

            var pd = sessionService.createSession(
                    player.getUniqueId().toString(),
                    new SessionCreateRequest(
                            ProxySessionService.hostname,
                            player.getUsername(),
                            player.getRemoteAddress().getAddress().getHostAddress(),
                            new SessionCreateRequest.Skin(skinTexture, skinSignature),
                            player.getRawVirtualHost().orElse(null)
                    )
            );
            playersJustJoined.add(player.getUniqueId());
            logger.info("created session (v2) for {}: {}", player.getUsername(), pd);
        } catch (ProxySessionService.MaintenanceException ignored) {
            event.setResult(ResultedEvent.ComponentResult.denied(MAINTENANCE));
        } catch (ProxySessionService.BannedException error) {
            event.setResult(ResultedEvent.ComponentResult.denied(buildBannedMessage(error.getContent())));
        } catch (Exception e) {
            logger.error("failed to create session (v2) for {}", player.getUsername(), e);
            event.setResult(LoginEvent.ComponentResult.denied(Component.text("failed to create session")));
        }
    }

    private @NotNull Component buildBannedMessage(@NotNull JsonObject error) {
        return Component.translatable("You are banned");
    }

    @Subscribe
    public void handlePluginMessage(@NotNull PluginMessageEvent event) {
        logger.info("plugin message: {}", event.getIdentifier());
        if (TRANSFER_MESSAGE_ID.equals(event.getIdentifier())) {
            handleTransfer(event);
        } else if (RESOURCE_PACK_MESSAGE_ID.equals(event.getIdentifier())) {
            handleResourcePack(event);
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

    @Subscribe
    public void handlePostConnect(@NotNull ServerPostConnectEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        if (!playersJustJoined.contains(playerId)) return;

        playersJustJoined.remove(playerId);
        playerConnectAttempts.remove(playerId);
        System.out.println(event.getPlayer().getUniqueId() + " POST JOIN");
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

        var reason = event.getServerKickReason().orElse(null);
        if (reason != null) {
            // TODO: This feels like a bad way to do this. What's the proper way?
            var text = PlainTextComponentSerializer.plainText().serialize(reason).toLowerCase(Locale.ROOT);
            if (text.contains("banned") || text.contains("kicked") || text.contains("version")) {
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(reason));
                return;
            }
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
            return;
        }

    }

    private @Nullable GameProfile.Property getGPProperty(@NotNull GameProfile gp, @NotNull String name) {
        return gp.getProperties().stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

}
