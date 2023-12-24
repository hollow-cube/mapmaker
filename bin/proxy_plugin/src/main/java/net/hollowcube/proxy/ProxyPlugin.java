package net.hollowcube.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.player.SessionServiceImpl;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

@Plugin(id = "hc-proxy", name = "hollowcube proxy plugin", version = "1.0", authors = "hollow cube")
public class ProxyPlugin {
    private final Logger logger;
    private final ProxyServer proxy;

    private SessionService sessionService;

    @Inject
    public ProxyPlugin(@NotNull Logger logger, @NotNull ProxyServer proxy) {
        this.logger = logger;
        this.proxy = proxy;

        this.sessionService = new SessionServiceImpl("http://session-service:9124");

        logger.info("hello, world!!!!");
    }

    @Subscribe
    public void handleLogin(@NotNull LoginEvent event) {
        var player = event.getPlayer();
        try {
            var pd = sessionService.createSessionV2(
                    player.getUniqueId().toString(),
                    player.getUsername(),
                    player.getRemoteAddress().getAddress().getHostAddress()
            );
            logger.info("created session (v2) for {}: {}", player.getUsername(), pd);
        } catch (Exception e) {
            logger.error("failed to create session (v2) for {}", player.getUsername(), e);
            event.setResult(LoginEvent.ComponentResult.denied(Component.text("failed to create session")));
        }
    }

    @Subscribe
    public void handleDisconnect(@NotNull DisconnectEvent event) {
        var playerId = event.getPlayer().getUniqueId().toString();
        try {
            sessionService.deleteSessionV2(playerId);
            logger.info("deleted session (v2) for {}", playerId);
        } catch (Exception e) {
            logger.error("failed to delete session (v2) for {}", playerId, e);
        }
    }

    @Subscribe
    public void handleInitialServer(PlayerChooseInitialServerEvent event) {
        var rs = proxy.createRawRegisteredServer(new ServerInfo("hub-minecraft", new InetSocketAddress("hub-minecraft", 25565)));
        event.setInitialServer(rs);
    }

//    @Subscribe
//    public void handleKickedFromServer(@NotNull KickedFromServerEvent event) {
//        if (event.kickedDuringServerConnect()) return;
//
//        logger.info("kicked from server: {}", event.getServer().getServerInfo().getName());
//        var rs = proxy.createRawRegisteredServer(new ServerInfo("hub-minecraft", new InetSocketAddress("hub-minecraft", 25565)));
//        event.setResult(KickedFromServerEvent.RedirectPlayer.create(rs));
//    }

}
