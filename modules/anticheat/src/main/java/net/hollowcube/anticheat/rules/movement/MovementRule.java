package modules.anticheat.src.main.java.net.hollowcube.anticheat.rules.movement;

import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatNotifier;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatRule;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

public abstract class MovementRule implements AntiCheatRule {

    @Override
    @MustBeInvokedByOverriders
    public void onInitialize(@NotNull GlobalEventHandler events, @NotNull AntiCheatNotifier notifier) {
        events.addListener(PlayerMoveEvent.class, event -> {
            if (canBypassMovementChecks(event.getPlayer())) return;
            onPlayerMove(event, notifier);
        });
    }

    protected abstract void onPlayerMove(@NotNull PlayerMoveEvent event, @NotNull AntiCheatNotifier notifier);

    private static boolean canBypassMovementChecks(@NotNull Player player) {
        if (player.isAllowFlying()) return true;
        if (player.getGameMode() == GameMode.CREATIVE) return true;
        if (player.getGameMode() == GameMode.SPECTATOR) return true;
        return false;
    }
}
