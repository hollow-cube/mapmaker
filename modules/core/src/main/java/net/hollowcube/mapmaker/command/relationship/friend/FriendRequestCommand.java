package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentLiteral;
import net.hollowcube.command.arg.ArgumentWord;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.FriendRequest;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FriendRequestCommand extends CommandDsl {
    private final Argument<String> targetArg;
    private final Argument<String> directionArg = Argument.Word("direction").with("outgoing", "incoming");

    private final PlayerService playerService;

    public FriendRequestCommand(@NotNull PlayerService playerService) {
        super("request");
        this.playerService = playerService;

        this.targetArg = CoreArgument.AnyPlayerId("target", playerService);

        this.addSyntax(playerOnly(this::execList), new ArgumentLiteral("list"), this.directionArg);
        this.addSyntax(playerOnly(this::execRemove), new ArgumentLiteral("remove"),
                       this.targetArg); // removes a request bidirectionally
    }

    private void execList(@NotNull Player player, @NotNull CommandContext context) {
        String directionValue = context.get(this.directionArg);
        boolean incoming = directionValue.equals("incoming");

        List<FriendRequest> requests = this.playerService.getFriendRequests(player.getUuid().toString(), incoming);
        TextComponent.Builder builder = Component.text()
            .append(Component.translatable("command.friend.request.list.header"));
        for (FriendRequest request : requests) {
            builder.appendNewline().append(Component.translatable("command.friend.request.list.line", Component.text(request.username())));
        }

        player.sendMessage(builder.build());
    }

    private void execRemove(@NotNull Player player, @NotNull CommandContext context) {
        var targetId = context.get(this.targetArg);
        if (targetId == null) return;
        if (targetId.equals(player.getUuid().toString())) {
            player.sendMessage(Component.translatable("command.friend.request.remove.self"));
            return;
        }

        var targetRaw = context.getRaw(this.targetArg);

        try {
            FriendRequest deletedReq = this.playerService.deleteFriendRequest(player.getUuid().toString(), targetId, true);
            // todo we can use deletedReq to indicate the direction
            player.sendMessage(Component.translatable("command.friend.request.remove.success", Component.text(deletedReq.username())));
        } catch (PlayerService.NotFoundError ex) {
            player.sendMessage(
                Component.translatable("command.friend.request.remove.not_requested", Component.text(targetRaw)));
        }
    }
}
