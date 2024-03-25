package net.hollowcube.mapmaker.map.command.utility;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.item.handler.ExtraItemTags;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class PHeadCommand extends CommandDsl {
    private final Argument<String> nameArg = Argument.Word("name")
            .description("The player to obtain a head of");

    @Inject
    public PHeadCommand() {
        super("phead", "skull");

        description = "Gives you the head of a player";

        setCondition(mapFilter(false, true, false));
        addSyntax(playerOnly(this::handleGiveHead), nameArg);
    }

    private void handleGiveHead(@NotNull Player player, @NotNull CommandContext context) {
        var name = context.get(nameArg);

        FutureUtil.submitVirtual(() -> {
            var skin = PlayerSkin.fromUsername(name); // Blocking call
            var so = new ExtraItemTags.SkullOwner(null, name, skin);

            var itemStack = ItemStack.builder(Material.PLAYER_HEAD)
                    .meta(meta -> meta.set(ExtraItemTags.SKULL_OWNER, so))
                    .build();
            player.getInventory().addItemStack(itemStack);
        });
    }
}
