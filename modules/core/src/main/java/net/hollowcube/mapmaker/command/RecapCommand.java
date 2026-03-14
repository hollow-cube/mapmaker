package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.components.TranslatableBuilder;
import net.hollowcube.mapmaker.feature.FeatureFlag;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

public class RecapCommand extends CommandDsl {

    public static FeatureFlag FLAG = FeatureFlag.of("hub.recaps");

    private final PlayerService players;

    public RecapCommand(PlayerService players) {
        super("recap");

        this.players = players;
        this.description = "Get a link to your Hollow Cube 2025 recap";
        this.category = CommandCategories.GLOBAL;

        this.setCondition(CoreCommandCondition.playerFeature(FLAG));

        addSyntax(playerOnly(this::invoke));
    }

    private void invoke(Player player, CommandContext context) {
        var recap = this.players.getRecap(player.getUuid().toString(), 2025);
        if (recap != null) {
            player.sendMessage(TranslatableBuilder.of("commands.recap.generated").with(recap).build());
        } else {
            player.sendMessage(Component.translatable("commands.recap.unavailable"));
        }
    }
}
