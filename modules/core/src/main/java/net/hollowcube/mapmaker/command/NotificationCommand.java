package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentBool;
import net.hollowcube.command.arg.ArgumentInt;
import net.hollowcube.command.arg.ArgumentWord;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.gui.notifications.NotificationListView;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class NotificationCommand extends CommandDsl {

    private final ServiceContext services;

    public NotificationCommand(@NotNull ServiceContext services) {
        super("notifications");
        this.category = CommandCategories.GLOBAL;
        this.description = "View your notifications";

        this.services = services;

        addSyntax(playerOnly(this::execute));
        if (ServerRuntime.getRuntime().isDevelopment()) {
            addSubcommand(new Notify());
        }
    }

    public void execute(@NotNull Player player, @NotNull CommandContext context) {
        Panel.open(player, new NotificationListView(this.services));
    }


    private class Notify extends CommandDsl {

        private final Argument<String> playerArg = CoreArgument.AnyPlayerId("player", services.players());
        private final ArgumentWord categoryArg = Argument.Word("category");
        private final ArgumentWord keyArg = Argument.Word("key");
        private final ArgumentInt expiresInt = Argument.Int("expiresIn").min(-1);
        private final ArgumentBool replaceUnreadArg = Argument.Bool("replaceUnread");

        public Notify() {
            super("notify");

            this.setCondition(staffPerm(Permission.GENERIC_STAFF));

            this.category = CommandCategories.GLOBAL;
            this.description = "Send a notification to a player";

            this.addSyntax(this::execute, playerArg, categoryArg, keyArg, expiresInt, replaceUnreadArg);
        }

        public void execute(@NotNull CommandSender sender, @NotNull CommandContext context) {
            var target = context.get(playerArg);
            var category = context.get(categoryArg);
            var key = context.get(keyArg);
            var expiresIn = context.get(expiresInt);
            var replaceUnread = context.get(replaceUnreadArg);

            services.players().createNotification(
                target, category, key, null, expiresIn < 0 ? null : expiresIn, replaceUnread
            );
        }
    }
}
