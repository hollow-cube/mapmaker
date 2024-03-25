package net.hollowcube.mapmaker.map.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.item.ItemUtils;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class GiveCommand extends CommandDsl {
    private final Argument<ItemStack> itemArg = ItemRegistry.Argument("item")
            .description("The item to give yourself");
    private final Argument<Integer> amountArg = Argument.Int("count")
            .clamp(1, 64).defaultValue(1)
            .description("The amount of the item to give yourself");

    public GiveCommand() {
        super("give");

        description = "Gives you any item or custom item in the game";
        examples = List.of("/give mapmaker:checkpoint_plate", "/give minecraft:grass_block");

        setCondition(mapFilter(false, true, false));
        addSyntax(playerOnly(this::handleGiveItem), itemArg);
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
