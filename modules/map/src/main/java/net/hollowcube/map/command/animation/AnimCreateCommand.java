package net.hollowcube.map.command.animation;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AnimCreateCommand extends CommandDsl {
    private final Argument<String> nameArg = Argument.Word("name");

    public AnimCreateCommand() {
        super("create");

        addSyntax(playerOnly(this::handleCreateNamed), nameArg);
    }

    private void handleCreateNamed(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if (!(world instanceof EditingMapWorld editingWorld)) return;
        var builder = editingWorld.animationBuilder();

        var name = FontUtil.stripNonAlphanumeric(context.get(nameArg));
        if (name.isEmpty()) {
            player.sendMessage("Invalid name");
            return;
        }
        builder.begin(name);
    }

}
