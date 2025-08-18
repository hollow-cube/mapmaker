package net.hollowcube.mapmaker.editor.command.utility;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.hollowcube.mapmaker.util.ItemUtils;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.validate.Check;

import java.util.List;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;
import static net.kyori.adventure.text.Component.translatable;

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

        setCondition(builderOnly());
        addSyntax(playerOnly(this::handleGiveItem), itemArg);
        addSyntax(playerOnly(this::handleGiveItem), itemArg, amountArg);
    }

    private void handleGiveItem(Player player, CommandContext context) {
        var itemStack = context.get(itemArg);
        Check.notNull(itemStack, "Item stack is null");
        var count = context.get(amountArg);

        itemStack = itemStack.withAmount(count);
        boolean result = player.getInventory().addItemStack(itemStack, TransactionOption.ALL_OR_NOTHING);
        if (!result) {
            player.sendMessage(translatable("command.give.not_enough_space"));
            return;
        }

        var itemName = itemStack.get(DataComponents.CUSTOM_NAME);
        if (itemName == null) itemName = ItemUtils.translation(itemStack.material());
        player.sendMessage(translatable("command.give.success", itemName.hoverEvent(itemStack.asHoverEvent())));
    }
}
