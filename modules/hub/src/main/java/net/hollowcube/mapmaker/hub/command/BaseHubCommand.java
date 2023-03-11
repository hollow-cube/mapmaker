package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A command which is only available in a mapmaker hub world.
 */
class BaseHubCommand extends Command {
    public BaseHubCommand(@NotNull String name, @Nullable String... aliases) {
        super(name, aliases);

        setCondition(this::isInHub);
    }

    public void addCondition(@NotNull CommandCondition condition) {
        var oldCondition = getCondition();
        setCondition(oldCondition == null ? condition : andConditions(oldCondition, condition));
    }

    private @NotNull CommandCondition andConditions(@NotNull CommandCondition a, @NotNull CommandCondition b) {
        return (sender, command) -> a.canUse(sender, command) && b.canUse(sender, command);
    }

    private boolean isInHub(@Nullable CommandSender sender, @Nullable String unused) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        var instance = player.getInstance();
        if (instance == null) return false;
        return instance.hasTag(HubWorld.MARKER);
    }
}
