package net.hollowcube.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandNode;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand extends CommandDsl {
    private static final TextColor DARK_GRAY = TextColor.color(0x696969);
    private static final TextColor WHITE_GRAY = TextColor.color(0xF2F2F2);

    private final Argument<String> commandArg = Argument.GreedyString("command");

    private final CommandReflection reflect;

    public HelpCommand(@NotNull CommandManager commandManager) {
        super("help", "h");
        this.reflect = commandManager.reflect();

        addSyntax(this::handleShowCommandList);
//        addSyntax(this::handleShowCommandDetail, commandArg);
    }

    private void handleShowCommandList(@NotNull CommandSender sender, @NotNull CommandContext context) {
        var builder = Component.text();
        boolean first = true;
        for (var entry : reflect.commands(sender, false)) {
            if (!first) builder.appendNewline();
            first = false;

            builder.append(Component.text(entry.getKey())
                    .hoverEvent(HoverEvent.showText(createDetail(entry.getKey(), entry.getValue(), sender))));
        }
        sender.sendMessage(builder.build());
    }

    private void handleShowCommandDetail(@NotNull CommandSender sender, @NotNull CommandContext context) {

    }

    private @NotNull Component createDetail(@NotNull String name, @NotNull CommandNode node, @NotNull CommandSender sender) {
        var args = new ArrayList<List<String>>();
        collectChildren(args, node, sender, 0);

        var builder = Component.text();
        builder.append(Component.text("usage: /" + name));
        for (var arg : args) {
            builder.append(Component.text(" <", DARK_GRAY));

            var iter = arg.iterator();
            while (iter.hasNext()) {
                builder.append(Component.text(iter.next(), WHITE_GRAY));
                if (iter.hasNext()) {
                    builder.append(Component.text("|", DARK_GRAY));
                }
            }

            builder.append(Component.text(">", DARK_GRAY));
        }

        return builder.build();
    }

    private void collectChildren(@NotNull List<List<String>> args, @NotNull CommandNode node, @NotNull CommandSender sender, int depth) {
        var children = reflect.children(node, sender);
        if (children.isEmpty()) return;

        List<String> list;
        if (args.size() <= depth) {
            list = new ArrayList<>();
            args.add(list);
        } else {
            list = args.get(depth);
        }

        // If there is only one child it should be added, and we can inspect those children
        if (children.size() == 1) {
            var entry = children.iterator().next();
            list.add(entry.getKey().id());
            collectChildren(args, entry.getValue(), sender, depth + 1);
            return;
        }

        // Otherwise add all children but do not go any deeper.
        for (var entry : children) {
            list.add(entry.getKey().id());
        }
    }

    /*

    DESCRIPTION HERE
    usage: /tp <target|location>
    arguments:
     target: The player to teleport to
     location: The location to teleport to
    examples:
     /tp notmattw
     /tp ~ ~20 ~

    usage: /team <add|remove|blah|blah>
    subcommands:
     add: Add a new team

     */

}
