package net.hollowcube.server.events.once;

import net.hollowcube.server.MapMaker;
import net.minestom.server.instance.block.Block;

public class ServerStartupEvent {
    public static void onServerStartup() {
        // Temp action
        MapMaker.getInstance().getWorldInstanceManager().getBaseInstance().setBlock(0, 58, 0, Block.BEDROCK);
    }
}
