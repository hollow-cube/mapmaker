package net.hollowcube.mapmaker.command.relationship;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentLiteral;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.CoreCommandCondition;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.BlockedPlayer;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlockCommand extends CommandDsl {
    private final Argument<String> targetArg;

    private final PlayerService playerService;

    public BlockCommand(@NotNull PlayerService playerService) {
        super("block");
        this.playerService = playerService;
        this.category = CommandCategories.SOCIAL;
        this.description = "Blocks a player";
        this.examples = List.of("/block SethPRG");

        this.targetArg = CoreArgument.AnyPlayerId("target", playerService).description("The player to block");

        this.setCondition(CoreCommandCondition.playerFeature(RelationshipFeatureFlag.FLAG));

        this.addSyntax(playerOnly(this::execListBlocks), new ArgumentLiteral("list"));
        this.addSyntax(playerOnly(this::execBlock), this.targetArg);
    }

    private void execBlock(@NotNull Player player, @NotNull CommandContext context) {
        var targetId = context.get(this.targetArg);
        if (targetId == null) return;
        if (targetId.equals(player.getUuid().toString())) {
            player.sendMessage(Component.translatable("command.block.self"));
            return;
        }

        try {
            this.playerService.blockPlayer(player.getUuid().toString(), targetId);
            player.sendMessage(Component.translatable("command.block.success", Component.text(targetId)));
        } catch (PlayerService.AlreadyExistsError ex) {
            player.sendMessage(Component.translatable("command.block.already_blocked", Component.text(targetId)));
        }
    }

    private void execListBlocks(@NotNull Player player, @NotNull CommandContext context) {
        List<BlockedPlayer> blocks = this.playerService.getBlockedPlayers(player.getUuid().toString());

        TextComponent.Builder builder = Component.text().append(Component.translatable("command.block.list.header"));
        for (BlockedPlayer block : blocks) {
            builder.appendNewline().append(Component.translatable("command.block.list.line", Component.text(block.username())));
        }
        player.sendMessage(builder.build());
    }
}
