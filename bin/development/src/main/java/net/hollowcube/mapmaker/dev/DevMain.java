package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevMain {
    private static final Logger logger = LoggerFactory.getLogger(DevMain.class);

    static void main(String[] args) throws Exception {
        // Escape hatch for clients without a mojang session (eg the anticheat packet-tap test client).
        if (Boolean.parseBoolean(System.getenv("MAPMAKER_DEV_OFFLINE_AUTH"))) {
            // Must match MapServerInitializer, ServerFlag reads these when the server is initialized.
            MapServerInitializer.SYSTEM_PROPERTIES.forEach(System::setProperty);
            MapServerInitializer.preInitializedServer = MinecraftServer.init(new Auth.Offline());
            logger.info("dev server running with offline auth (MAPMAKER_DEV_OFFLINE_AUTH)");
        }

        MapServerInitializer.run(DevServer::new, args);
    }

}
