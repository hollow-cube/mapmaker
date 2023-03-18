package net.hollowcube.map.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.item.ItemManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GiveCommand extends BaseMapCommand {
    private static final Argument<ItemStack> itemArgument = ItemManager.ARGUMENT;

    public GiveCommand() {
        super(true, "give");

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /give <item>"));

        addSyntax(this::giveItem, itemArgument);
    }

    private void giveItem(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var item = context.get(itemArgument);
        if (item == null) {
            player.sendMessage("No such item: " + context.getRaw(itemArgument));
            return;
        }

        boolean result = player.getInventory().addItemStack(item);
        if (!result) {
            sender.sendMessage("You do not have enough space in your inventory to receive this item");
        }

        Component itemComponent;
        if (item.getDisplayName() != null) itemComponent = item.getDisplayName();
        else itemComponent = Component.text(item.material().name()); //todo should use vanilla translation for item
        sender.sendMessage(Component.text("Gave one ").append(itemComponent.hoverEvent(item.asHoverEvent())));
    }
}
