package net.hollowcube.map.command.utility;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.map.item.ItemUtils;
import net.hollowcube.map.lang.MapMessages;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class GiveCommand extends Command {
    private final Argument<ItemStack> itemArg = ItemRegistry.Argument("item");

    public GiveCommand() {
        super("give");
        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::handleGiveItem), itemArg);
    }

    private void handleGiveItem(@NotNull Player player, @NotNull CommandContext context) {
        var itemStack = context.get(itemArg);
        Check.notNull(itemStack, "Item stack is null");

        boolean result = player.getInventory().addItemStack(itemStack, TransactionOption.ALL_OR_NOTHING);
        if (!result) {
            player.sendMessage(MapMessages.COMMAND_GIVE_NOT_ENOUGH_SPACE);
            return;
        }

        var itemComponent = itemStack.getDisplayName() != null
                ? itemStack.getDisplayName()
                : ItemUtils.translation(itemStack.material());
        player.sendMessage(MapMessages.COMMAND_GIVE_SUCCESS.with(
                itemComponent.hoverEvent(itemStack.asHoverEvent())
        ));
    }
}
