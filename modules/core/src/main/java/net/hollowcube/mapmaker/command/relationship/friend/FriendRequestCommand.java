package net.hollowcube.mapmaker.command.relationship.friend;

import java.util.UUID;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentLiteral;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.ipc.player.FriendRequest;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FriendRequestCommand extends CommandDsl {
    private final Argument<String> targetArg;
    private final Argument<String> directionArg = Argument.Word("direction").with("outgoing", "incoming").defaultValue("incoming");
    private final Argument<Integer> pageArg = Argument.Int("page").min(1).defaultValue(1);

    private final SocialService social;

    public FriendRequestCommand(@NotNull PlayerService players, @NotNull SocialService social) {
        super("request");
        this.social = social;

        this.targetArg = CoreArgument.AnyPlayerId("target", players);

        this.addSyntax(playerOnly(this::execList), new ArgumentLiteral("list"));
        this.addSyntax(playerOnly(this::execList), new ArgumentLiteral("list"), this.directionArg);
        this.addSyntax(playerOnly(this::execList), new ArgumentLiteral("list"), this.directionArg, this.pageArg);
        this.addSyntax(playerOnly(this::execRemove), new ArgumentLiteral("remove"),
                       this.targetArg); // removes a request bidirectionally
    }

    private void execList(@NotNull Player player, @NotNull CommandContext context) {
        String directionValue = context.get(this.directionArg);
        boolean incoming = directionValue.equals("incoming");
        int page = context.get(this.pageArg);

        var requests = incoming
            ? this.social.incomingFriendRequests(player.getUuid(), page - 1, 10)
            : this.social.outgoingFriendRequests(player.getUuid(), page - 1, 10);
        int pageCount = Math.ceilDiv(requests.count(), 10);

        if (pageCount == 0) {
            player.sendMessage(Component.translatable("command.friend.request.list.empty." + directionValue));
            return;
        }

        TextComponent.Builder builder = Component.text()
            .append(Component.translatable("command.friend.request.list.header." + directionValue, Component.text(page), Component.text(pageCount)));
        for (FriendRequest request : requests.results()) {
            var name = request.player().displayName().render();
            builder.appendNewline().append(
                Component.translatable("command.friend.request.list.line." + directionValue, name, Component.text(request.player().username()))
            );
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
        var deleted = this.social.deleteFriendRequest(player.getUuid(), UUID.fromString(targetId), true);
        if (deleted == null) {
            player.sendMessage(Component.translatable("command.friend.request.remove.not_requested", Component.text(targetRaw)));
        } else {
            player.sendMessage(Component.translatable("command.friend.request.remove.success", deleted.player().displayName().render()));
        }
    }
}
