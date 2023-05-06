package net.hollowcube.map.command;

import net.hollowcube.map.world.MapWorld;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A command which is only available in a mapmaker map (does not restrict to playing or editing)
 */
public class BaseMapCommand extends Command {
    private final boolean editOnly;

    public BaseMapCommand(@NotNull String name, @Nullable String... aliases) {
        this(false, name, aliases);
    }

    public BaseMapCommand(boolean editOnly, @NotNull String name, @Nullable String... aliases) {
        super(name, aliases);
        this.editOnly = editOnly;

        setCondition(createMapCondition(editOnly));
    }

    public static @NotNull CommandCondition createMapCondition(boolean editOnly) {
        return (sender, unused) -> {
            if (!(sender instanceof Player player)) {
                return false;
            }
            var mapWorld = MapWorld.forPlayerOptional(player);
            if (mapWorld == null) return false;
            // If not edit only we can return true immediately (allowed no matter the map mode)
            if (!editOnly) return true;

            return (mapWorld.flags() & MapWorld.FLAG_EDITING) != 0;
        };
    }
}
