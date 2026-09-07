package net.hollowcube.mapmaker.command.relationship;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentLiteral;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.ipc.player.BlockResult;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class BlockCommand extends CommandDsl {
    private final Argument<String> targetArg;
    private final Argument<Integer> pageArg = Argument.Int("page").min(1).defaultValue(1);

    private final SocialService social;

    public BlockCommand(@NotNull PlayerService players, @NotNull SocialService social) {
        super("block");
        this.social = social;

        this.category = CommandCategories.SOCIAL;
        this.description = "Blocks a player";
        this.examples = List.of("/block SethPRG");

        this.targetArg = CoreArgument.AnyPlayerId("target", players).description("The player to block");

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

        var key = switch (this.social.block(player.getUuid(), UUID.fromString(targetId))) {
            case BlockResult.Blocked _ -> "command.block.success";
            case BlockResult.AlreadyBlocked _ -> "command.block.already_blocked";
            case BlockResult.TargetIsStaff _ -> "command.block.cannot_target_staff";
            case BlockResult.Unknown _ -> "generic.unknown_error";
        };
        player.sendMessage(Component.translatable(key, Component.text(targetRaw)));
    }

    private void execListBlocks(@NotNull Player player, @NotNull CommandContext context) {
        int page = context.get(this.pageArg);
        var blocks = this.social.blocks(player.getUuid(), page - 1, 10);
        int pageCount = Math.ceilDiv(blocks.count(), 10);

        if (pageCount == 0) {
            player.sendMessage(Component.translatable("command.block.list.empty"));
            return;
        }

        TextComponent.Builder builder = Component.text().append(Component.translatable("command.block.list.header", Component.text(page), Component.text(pageCount)));
        for (var block : blocks.results()) {
            var name = block.target().displayName().render();
            builder.appendNewline().append(Component.translatable("command.block.list.line", name, Component.text(block.target().username())));
        }
        player.sendMessage(builder.build());
    }
}
