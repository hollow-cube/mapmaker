package net.hollowcube.mapmaker.command.relationship;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentLiteral;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.CoreCommandCondition;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.BlockedPlayer;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlockCommand extends CommandDsl {
    private final Argument<String> targetArg;
    private final Argument<Integer> pageArg = Argument.Int("page").min(1).defaultValue(1);

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
        this.addSyntax(playerOnly(this::execListBlocks), new ArgumentLiteral("list"), this.pageArg);
        this.addSyntax(playerOnly(this::execBlock), this.targetArg);
    }

    private void execBlock(@NotNull Player player, @NotNull CommandContext context) {
        var targetId = context.get(this.targetArg);
        if (targetId == null) return;
        if (targetId.equals(player.getUuid().toString())) {
            player.sendMessage(Component.translatable("command.block.self"));
            return;
        }
        var targetRaw = context.getRaw(this.targetArg);

        try {
            this.playerService.blockPlayer(player.getUuid().toString(), targetId);
            player.sendMessage(Component.translatable("command.block.success", Component.text(targetRaw)));
        } catch (PlayerService.AlreadyExistsError ex) {
            player.sendMessage(Component.translatable("command.block.already_blocked", Component.text(targetRaw)));
        } catch (PlayerService.BadRequestError ex) {
            player.sendMessage(Component.translatable("command.block.cannot_target_staff", Component.text(targetRaw)));
        }
    }

    private void execListBlocks(@NotNull Player player, @NotNull CommandContext context) {
        int page = context.get(this.pageArg);
        PlayerService.Page<BlockedPlayer> blocks = this.playerService.getBlockedPlayers(player.getUuid().toString(), new PlayerService.Pageable(page, 10));
        int pageCount = Math.ceilDiv(blocks.totalItems(), 10);

        if (pageCount == 0) {
            player.sendMessage(Component.translatable("command.block.list.empty"));
            return;
        }

        TextComponent.Builder builder = Component.text().append(Component.translatable("command.block.list.header", Component.text(blocks.page()), Component.text(pageCount)));
        for (BlockedPlayer block : blocks.items()) {
            DisplayName displayName = this.playerService.getPlayerDisplayName2(block.playerId());
            Component username = displayName.asComponent();
            builder.appendNewline().append(Component.translatable("command.block.list.line", username, Component.text(block.username())));
        }
        player.sendMessage(builder.build());
    }
}
