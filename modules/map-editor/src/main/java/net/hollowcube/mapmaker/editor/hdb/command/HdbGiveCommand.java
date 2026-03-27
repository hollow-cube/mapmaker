package net.hollowcube.mapmaker.editor.hdb.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.api.hdb.HeadDatabaseClient;
import net.hollowcube.mapmaker.editor.hdb.HdbMessages;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class HdbGiveCommand extends CommandDsl {
    private final Argument<String> queryArg = Argument.GreedyString("query")
        .defaultValue("").description("The head to search for");

    private final HeadDatabaseClient hdb;

    public HdbGiveCommand(@NotNull HeadDatabaseClient hdb) {
        super("give");
        this.hdb = hdb;

        addSyntax(playerOnly(this::handleGiveHead));
        addSyntax(playerOnly(this::handleGiveHead), queryArg);
    }

    private void handleGiveHead(@NotNull Player player, @NotNull CommandContext context) {
        var query = context.get(queryArg);

        var results = hdb.getHeads(query.replace("_", " "), 0, 1);
        if (results.isEmpty()) {
            player.sendMessage(HdbMessages.COMMAND_GIVE_NO_RESULT.with(query));
            return;
        }

        var itemStack = results.first().createItemStack();
        PlayerUtil.giveItem(player, itemStack);
        player.sendMessage(HdbMessages.COMMAND_GIVE_RESULT.with(
            Objects.requireNonNull(itemStack.get(DataComponents.CUSTOM_NAME, Component.empty())).hoverEvent(itemStack.asHoverEvent())));
    }

}
