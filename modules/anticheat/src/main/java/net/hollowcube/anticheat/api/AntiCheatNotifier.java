package modules.anticheat.src.main.java.net.hollowcube.anticheat.api;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface AntiCheatNotifier {

    void sendNotification(
            @NotNull Player player,
            @NotNull String id,
            @NotNull String message
    );
}
