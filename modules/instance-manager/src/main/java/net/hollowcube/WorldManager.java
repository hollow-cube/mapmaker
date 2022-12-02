package net.hollowcube;

import net.hollowcube.commands.CommandLoader;
import net.hollowcube.database.PostgreSQLManager;
import net.hollowcube.database.SeaweedFS;
import net.minestom.server.MinecraftServer;

public class WorldManager {
    private static WorldManager instance;
    private static PostgreSQLManager PostgreSQL;
    private static SeaweedFS seaweedFS;

    public static WorldManager getInstance() {
        return instance;
    }

    // TODO init with args (file system, database)
    public static void init() {
        instance = new WorldManager();
        PostgreSQLManager.init();
        PostgreSQL = PostgreSQLManager.get();
        seaweedFS = new SeaweedFS();
        //seaweedFS.connectToSeaweedFs();
        CommandLoader.registerCommands();
    }

    public PostgreSQLManager getPostgreSQL() {
        return PostgreSQL;
    }
}
