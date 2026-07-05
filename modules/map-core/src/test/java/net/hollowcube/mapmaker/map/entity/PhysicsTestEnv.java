package net.hollowcube.mapmaker.map.entity;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

/** Boots a bare Minestom process once and creates disposable flat instances for physics tests. */
public final class PhysicsTestEnv {

    private PhysicsTestEnv() {
    }

    /** The walkable surface of {@link #createFlatInstance()} floors. */
    public static final int FLOOR_Y = 40;

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        MinecraftServer.init();
        initialized = true;
    }

    public static @NotNull InstanceContainer createFlatInstance() {
        init();
        var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, FLOOR_Y, Block.STONE));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                instance.loadChunk(x, z).join();
            }
        }
        return instance;
    }
}
