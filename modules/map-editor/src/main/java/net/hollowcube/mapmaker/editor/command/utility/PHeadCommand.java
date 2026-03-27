package net.hollowcube.mapmaker.editor.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.MojangUtil;
import net.hollowcube.mapmaker.util.CoreSkulls;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

public class PHeadCommand extends CommandDsl {
    private final Argument<String> nameArg = Argument.Word("name")
            .description("The player to obtain a head of");

    public PHeadCommand() {
        super("phead", "skull");

        description = "Gives you the head of a player";

        setCondition(builderOnly());
        addSyntax(playerOnly(this::handleGiveHead), nameArg);
    }

    private void handleGiveHead(Player player, CommandContext context) {
        var name = context.get(nameArg);

        FutureUtil.submitVirtual(() -> {
            var skin = MojangUtil.getSkinFromUsername(name); // Blocking call
            var builder = ItemStack.builder(Material.PLAYER_HEAD);
            if (skin != null) builder.set(DataComponents.PROFILE, CoreSkulls.create(skin));
            player.getInventory().addItemStack(builder.build());
        });
    }
}
