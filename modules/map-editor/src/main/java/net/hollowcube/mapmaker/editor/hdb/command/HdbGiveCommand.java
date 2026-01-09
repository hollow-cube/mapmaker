package net.hollowcube.mapmaker.editor.hdb.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.editor.hdb.HdbMessages;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class HdbGiveCommand extends CommandDsl {
    private final Argument<String> queryArg = Argument.GreedyString("query")
            .defaultValue("").description("The head to search for");

    private final MapService maps;

    public HdbGiveCommand(@NotNull MapService maps) {
        super("give");
        this.maps = maps;

        addSyntax(playerOnly(this::handleGiveHead));
        addSyntax(playerOnly(this::handleGiveHead), queryArg);
    }

    private void handleGiveHead(@NotNull Player player, @NotNull CommandContext context) {
        var query = context.get(queryArg);

        var results = maps.getHeadsWithSearch(query.replace("_", " "), 0, 1).results();
        if (results.isEmpty()) {
            player.sendMessage(HdbMessages.COMMAND_GIVE_NO_RESULT.with(query));
            return;
        }

        var itemStack = results.getFirst().createItemStack();
        PlayerUtil.giveItem(player, itemStack);
        player.sendMessage(HdbMessages.COMMAND_GIVE_RESULT.with(
                Objects.requireNonNull(itemStack.get(DataComponents.CUSTOM_NAME, Component.empty())).hoverEvent(itemStack.asHoverEvent())));
    }

}
