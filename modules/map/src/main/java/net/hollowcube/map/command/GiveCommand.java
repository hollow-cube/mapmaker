package net.hollowcube.map.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.map.item.ItemUtils;
import net.hollowcube.map.lang.MapMessages;
import net.hollowcube.map.world.MapWorldNew;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GiveCommand extends BaseMapCommand {
    private static final Argument<@Nullable String> itemArg = ItemRegistry.Argument("item");

    public GiveCommand() {
        super(true, "give");

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /give <item>"));

        addSyntax(this::giveItem, itemArg);
    }

    private void giveItem(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var itemName = context.get(itemArg);
        if (itemName == null) {
            player.sendMessage(MapMessages.COMMAND_GIVE_UNKNOWN_ITEM.with(context.getRaw(itemArg)));
            return;
        }

        var itemRegistry = MapWorldNew.forPlayer(player).itemRegistry();
        var itemStack = itemRegistry.getItemStack(itemName, null);
        Check.notNull(itemStack, "Item stack is null");

        boolean result = player.getInventory().addItemStack(itemStack, TransactionOption.ALL_OR_NOTHING);
        if (!result) {
            sender.sendMessage(MapMessages.COMMAND_GIVE_NOT_ENOUGH_SPACE);
            return;
        }

        var itemComponent = itemStack.getDisplayName() != null
                ? itemStack.getDisplayName()
                : ItemUtils.translation(itemStack.material());
        sender.sendMessage(MapMessages.COMMAND_GIVE_SUCCESS.with(
                itemComponent.hoverEvent(itemStack.asHoverEvent())
        ));
    }
}
