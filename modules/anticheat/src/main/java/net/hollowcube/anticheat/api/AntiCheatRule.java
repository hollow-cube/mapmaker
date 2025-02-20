package modules.anticheat.src.main.java.net.hollowcube.anticheat.api;

import net.minestom.server.event.GlobalEventHandler;
import org.jetbrains.annotations.NotNull;

public interface AntiCheatRule {

    void onInitialize(@NotNull GlobalEventHandler events, @NotNull AntiCheatNotifier notifier);
}
