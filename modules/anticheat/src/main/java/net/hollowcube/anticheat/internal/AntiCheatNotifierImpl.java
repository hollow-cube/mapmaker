package modules.anticheat.src.main.java.net.hollowcube.anticheat.internal;

import modules.anticheat.src.main.java.net.hollowcube.anticheat.api.AntiCheatNotifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

// TODO Use posthog to log and kafka to send notifications to moderators
public class AntiCheatNotifierImpl implements AntiCheatNotifier {

    @Override
    public void sendNotification(@NotNull Player player, @NotNull String id, @NotNull String message) {
        var component = Component.empty()
                .append(Component.text("[AntiCheat]").color(NamedTextColor.RED))
                .appendSpace()
                .append(Component.text("(%s)".formatted(id)).color(NamedTextColor.YELLOW))
                .append(Component.text(message));
        player.sendMessage(component);
    }
}
