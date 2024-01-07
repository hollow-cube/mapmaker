package net.hollowcube.map.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument2;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.map.item.ItemUtils;
import net.hollowcube.map.lang.MapMessages;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class GiveCommand extends CommandDsl {
    private final Argument2<ItemStack> itemArg = ItemRegistry.Argument("item");
    private final Argument2<Integer> amountArg = Argument2.Int("count")
            .clamp(1, 64).defaultValue(1);

    public GiveCommand() {
        super("give");
        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::handleGiveItem), itemArg, amountArg);
    }

    private void handleGiveItem(@NotNull Player player, @NotNull CommandContext context) {
        var itemStack = context.get(itemArg);
        Check.notNull(itemStack, "Item stack is null");
        var count = context.get(amountArg);

        itemStack = itemStack.withAmount(count);
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
