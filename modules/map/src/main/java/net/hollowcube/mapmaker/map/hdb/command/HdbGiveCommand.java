package net.hollowcube.mapmaker.map.hdb.command;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.hdb.HdbMessages;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.hollowcube.mapmaker.map.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class HdbGiveCommand extends CommandDsl {
    private final Argument<String> queryArg = Argument.GreedyString("query")
            .defaultValue("").description("The head to search for");

    private final HeadDatabase hdb;

    @Inject
    public HdbGiveCommand(@NotNull HeadDatabase hdb) {
        super("give");
        this.hdb = hdb;

        addSyntax(playerOnly(this::handleGiveHead));
        addSyntax(playerOnly(this::handleGiveHead), queryArg);
    }

    private void handleGiveHead(@NotNull Player player, @NotNull CommandContext context) {
        if (!hdb.isLoaded()) {
            player.sendMessage(HdbMessages.GENERIC_UNLOADED);
            return;
        }

        var query = context.get(queryArg);

        var results = hdb.suggest(query.replace("_", " "), 1);
        if (results.isEmpty()) {
            player.sendMessage(HdbMessages.COMMAND_GIVE_NO_RESULT.with(query));
            return;
        }

        var itemStack = results.getFirst().createItemStack();
        PlayerUtil.smartAddItemStack(player, itemStack);
        player.sendMessage(HdbMessages.COMMAND_GIVE_RESULT.with(
                Objects.requireNonNull(itemStack.get(ItemComponent.CUSTOM_NAME, Component.empty())).hoverEvent(itemStack.asHoverEvent())));
    }

}
