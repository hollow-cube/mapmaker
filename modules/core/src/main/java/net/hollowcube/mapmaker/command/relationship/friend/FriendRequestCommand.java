package net.hollowcube.mapmaker.command.relationship.friend;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentLiteral;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.FriendRequest;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;

public class FriendRequestCommand extends CommandDsl {
    private final Argument<String> targetArg;
    private final Argument<String> directionArg = Argument.Word("direction").with("outgoing", "incoming").defaultValue("incoming");
    private final Argument<Integer> pageArg = Argument.Int("page").min(1).defaultValue(1);

    private final PlayerService playerService;

    public FriendRequestCommand(PlayerService playerService) {
        super("request");
        this.playerService = playerService;

        this.targetArg = CoreArgument.AnyPlayerId("target", playerService);

        this.addSyntax(playerOnly(this::execList), new ArgumentLiteral("list"));
        this.addSyntax(playerOnly(this::execList), new ArgumentLiteral("list"), this.directionArg);
        this.addSyntax(playerOnly(this::execList), new ArgumentLiteral("list"), this.directionArg, this.pageArg);
        this.addSyntax(playerOnly(this::execRemove), new ArgumentLiteral("remove"),
                       this.targetArg); // removes a request bidirectionally
    }

    private void execList(Player player, CommandContext context) {
        String directionValue = context.get(this.directionArg);
        boolean incoming = directionValue.equals("incoming");
        int page = context.get(this.pageArg);

        PlayerService.Page<FriendRequest> requests = this.playerService.getFriendRequests(player.getUuid().toString(), incoming, new PlayerService.Pageable(page, 10));
        int pageCount = Math.ceilDiv(requests.totalItems(), 10);

        if (pageCount == 0) {
            player.sendMessage(Component.translatable("command.friend.request.list.empty." + directionValue));
            return;
        }

        TextComponent.Builder builder = Component.text()
            .append(Component.translatable("command.friend.request.list.header." + directionValue, Component.text(requests.page()), Component.text(pageCount)));
        for (FriendRequest request : requests.items()) {
            DisplayName displayName = this.playerService.getPlayerDisplayName2(request.playerId());
            Component username = displayName.asComponent();
            builder.appendNewline().append(
                Component.translatable("command.friend.request.list.line." + directionValue, username, Component.text(request.username()))
            );
        }

        player.sendMessage(builder.build());
    }

    private void execRemove(Player player, CommandContext context) {
        var targetId = context.get(this.targetArg);
        if (targetId == null) return;
        if (targetId.equals(player.getUuid().toString())) {
            player.sendMessage(Component.translatable("command.friend.request.remove.self"));
            return;
        }

        var targetRaw = context.getRaw(this.targetArg);
        try {
            FriendRequest deletedReq = this.playerService.deleteFriendRequest(player.getUuid().toString(), targetId, true);
            Component targetDisplayName = playerService.getPlayerDisplayName2(deletedReq.playerId()).build();
            // todo we can use deletedReq to indicate the direction
            // that should be done, with a different message for outgoing and incoming
            player.sendMessage(Component.translatable("command.friend.request.remove.success", targetDisplayName));
        } catch (PlayerService.NotFoundError ex) {
            player.sendMessage(
                Component.translatable("command.friend.request.remove.not_requested", Component.text(targetRaw)));
        }
    }
}
