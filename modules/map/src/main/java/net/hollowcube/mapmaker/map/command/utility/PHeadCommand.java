package net.hollowcube.mapmaker.map.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.MojangUtil;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class PHeadCommand extends CommandDsl {
    private final Argument<String> nameArg = Argument.Word("name")
            .description("The player to obtain a head of");

    public PHeadCommand() {
        super("phead", "skull");

        description = "Gives you the head of a player";

        setCondition(mapFilter(false, true, false));
        addSyntax(playerOnly(this::handleGiveHead), nameArg);
    }

    private void handleGiveHead(@NotNull Player player, @NotNull CommandContext context) {
        var name = context.get(nameArg);

        FutureUtil.submitVirtual(() -> {
            var skin = MojangUtil.getSkinFromUsername(name); // Blocking call
            var builder = ItemStack.builder(Material.PLAYER_HEAD);
            if (skin != null) builder.set(DataComponents.PROFILE, new HeadProfile(skin));
            player.getInventory().addItemStack(builder.build());
        });
    }
}
