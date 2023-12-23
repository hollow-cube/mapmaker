package net.hollowcube.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

@Plugin(id = "hc-proxy", name = "hollowcube proxy plugin", version = "1.0", authors = "hollow cube")
public class ProxyPlugin {
    private final Logger logger;
    private final ProxyServer proxy;

    @Inject
    public ProxyPlugin(@NotNull Logger logger, @NotNull ProxyServer proxy) {
        this.logger = logger;
        this.proxy = proxy;

        logger.info("hello, world!!!!");
    }

    @Subscribe
    public void handleInitialServer(PlayerChooseInitialServerEvent event) {
        var rs = proxy.createRawRegisteredServer(new ServerInfo("hub-minecraft", new InetSocketAddress("hub-minecraft", 25565)));
        event.setInitialServer(rs);
    }

}
