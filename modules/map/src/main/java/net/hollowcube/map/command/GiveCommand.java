package net.hollowcube.map.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.map.item.ItemUtils;
import net.hollowcube.map.world.MapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.entity.Player;
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
            player.sendMessage("No such item: " + context.getRaw(itemArg));
            return;
        }

        var itemRegistry = MapWorld.fromInstance(player.getInstance()).itemRegistry();
        var itemStack = itemRegistry.getItemStack(itemName, null);
        // argument would return null if item not present, so safe to assume not null

        boolean result = player.getInventory().addItemStack(itemStack);
        if (!result) {
            sender.sendMessage("You do not have enough space in your inventory to receive this item");
        }

        var itemComponent = itemStack.getDisplayName() != null
                ? itemStack.getDisplayName()
                : ItemUtils.translation(itemStack.material());
        sender.sendMessage(Component.text("Gave one ").append(itemComponent.hoverEvent(itemStack.asHoverEvent())));
    }
}
