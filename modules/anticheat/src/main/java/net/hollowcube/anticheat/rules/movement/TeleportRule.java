package modules.anticheat.src.main.java.net.hollowcube.anticheat.rules.movement;

import com.google.auto.service.AutoService;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatNotifier;
import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatRule;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(AntiCheatRule.class)
public final class TeleportRule extends MovementRule {

    private static final int TELEPORT_THRESHOLD = 8;

    @Override
    public void onInitialize(@NotNull GlobalEventHandler events, @NotNull AntiCheatNotifier notifier) {
        super.onInitialize(events, notifier);
        events.addListener(PlayerMoveEvent.class, event -> onPlayerMove(event, notifier));
    }

    @Override
    protected void onPlayerMove(@NotNull PlayerMoveEvent event, @NotNull AntiCheatNotifier notifier) {
        var player = event.getPlayer();

        var oldPos = player.getPosition();
        var newPos = event.getNewPosition();
        var velocity = player.getVelocity();

        var xDiff = newPos.x() - oldPos.x();
        var yDiff = newPos.y() - oldPos.y();
        var zDiff = newPos.z() - oldPos.z();

        var xTeleported = Math.abs(xDiff) > TELEPORT_THRESHOLD && isVelocityMatchingOrNegligible(velocity.x(), xDiff);
        var yTeleported = Math.abs(yDiff) > TELEPORT_THRESHOLD && isVelocityMatchingOrNegligible(velocity.y(), yDiff);
        var zTeleported = Math.abs(zDiff) > TELEPORT_THRESHOLD && isVelocityMatchingOrNegligible(velocity.z(), zDiff);

        if (!xTeleported && !yTeleported && !zTeleported) return;

        notifier.sendNotification(player, "teleport", "Player has teleported a significant distance.");
    }

    private static boolean isVelocityMatchingOrNegligible(double velocity, double diff) {
        var diffSig = Math.signum(diff);
        var velSig = Math.signum(velocity);
        return velSig != diffSig || Math.abs(velocity) < 0.1;
    }

}
