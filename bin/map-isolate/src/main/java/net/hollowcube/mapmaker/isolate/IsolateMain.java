package net.hollowcube.mapmaker.isolate;

import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;

import java.util.List;

public class IsolateMain {
    public static String[] args;

    static {
        MapServerInitializer.SYSTEM_PROPERTIES.forEach(System::setProperty);
        System.setProperty("minestom.cached-packet", "false");
        MapServerInitializer.preInitializedServer = MinecraftServer.init(new Auth.Velocity("AsmMzWkjyrNvu5DprZWupQXy79mKFHK7d5Aj662YmVq4zEQns3RWb7NFpxvcTzgV"));
        // These are here to force some initialization ordering within the startup sequence... its a bit gross :)
        var ignoredTheClasses = List.of(AnvilLoader.class, DynamicChunk.class, Player.class, InstanceContainer.class);
    }

    public static void main(String[] args) {
        if (args.length > 0 && "noop".equals(args[0])) {
            System.out.println("Exiting because of noop argument.");
            System.exit(0);
        }

        IsolateMain.args = args;
        MapServerInitializer.run(MapIsolateServer::new, args);
    }
}
