package modules.anticheat.src.main.java.net.hollowcube.anticheat;

import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatRule;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.internal.AntiCheatNotifierImpl;
import net.minestom.server.MinecraftServer;

import java.util.ServiceLoader;

public final class AntiCheat {

    public static void init() {
        var notifier = new AntiCheatNotifierImpl();
        var events = MinecraftServer.getGlobalEventHandler();
        for (AntiCheatRule rule : ServiceLoader.load(AntiCheatRule.class)) {
            rule.onInitialize(events, notifier);
        }
    }

}
