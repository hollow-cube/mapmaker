package net.hollowcube.example;

import net.hollowcube.common.util.MojangUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.NoopServerBridge;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerSkin;
import net.hollowcube.mapmaker.session.Presence;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

import static net.hollowcube.mapmaker.map.MapPlayer.simpleMapPlayer;

public class ExampleServer extends AbstractMapServer {
    public static final String THE_MODE_ID = "924e5b96-c3dd-4615-b5a3-ca462d72c0d5";
    public static final String THE_MODE_ORG = "00efda58-6dd3-4401-9c28-d6b2bd507928";

    private static final Presence THE_GAME_PRESENCE = new Presence("game:game",
            // TODO: instanceId should be ServerRuntime.getRuntime().hostname(), but when running with a session
            //       service and not in kubernetes we need devserver
            "__game_unused__", "devserver", "game");

    private ExampleWorld world;

    public ExampleServer(ConfigLoaderV3 config) {
        super(config);

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("the-game-init")
                .addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin)
                .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
                .addListener(PlayerSpawnEvent.class, this::handleSpawn)
                .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager()
                .setPlayerProvider(simpleMapPlayer(commandManager()));

        this.world = new ExampleWorld(this, new MapData(THE_MODE_ID, THE_MODE_ORG));
    }

    @Override
    protected String name() {
        return "new-mode";
    }

    @Override
    protected ServerBridge createBridge() {
        return new NoopServerBridge();
    }

    protected void handlePreLogin(AsyncPlayerPreLoginEvent event) {
        // DevServer is not running behind a proxy, so we need to handle the proxy side of the session interaction
        // on our own here.
        // Note that we dont transfer here, its deferred to config phase (and reconfig)

        var profile = event.getGameProfile();
        var playerId = profile.uuid().toString();
        net.minestom.server.entity.PlayerSkin skin = MojangUtil.getSkinFromUuid(playerId);

        // TODO: when running with a proxy the session would be created by the proxy so this doesn't make sense.
        sessionService().createSession(
                playerId,
                "devserver-integrated",
                profile.name(),
                "127.0.0.1",
                new PlayerSkin(
                        OpUtils.map(skin, net.minestom.server.entity.PlayerSkin::textures),
                        OpUtils.map(skin, net.minestom.server.entity.PlayerSkin::signature)
                ),
                ProtocolVersions.getProtocolName(event.getConnection().getProtocolVersion()),
                event.getConnection().getProtocolVersion()
        );
    }

    protected void handleConfigPhase(AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();

        if (!transferPlayerSession(player, THE_GAME_PRESENCE)) {
            return;
        }

        // Disabled for now, needs the proxy running.
//        FutureUtil.getUnchecked(ResourcePackManager.sendResourcePack(player));
//        if (!player.isOnline()) return;

        // Setup the player in the world
        world.configurePlayer(event);
    }

    protected void handleSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;
        super.handleFirstSpawn(event.getPlayer());
    }

    protected void handleDisconnect(PlayerDisconnectEvent event) {
        super.handlePlayerDisconnect(event.getPlayer());
    }

}
