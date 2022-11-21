package net.hollowcube.map.command;

import net.hollowcube.map.MapServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;

public class MapDebugCommand extends Command {
    private final MapServer maps;

    public MapDebugCommand(@NotNull MapServer maps) {
        super("debug-map");
        this.maps = maps;

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /debug-map <subcommand>"));

        addSyntax(this::listActive, ArgumentType.Literal("list"));
    }

    private void listActive(CommandSender sender, CommandContext context) {
        System.gc();

        sender.sendMessage("Active maps (" + maps.instances.size() + "):");
        maps.instances.forEach((name, instance) -> sender.sendMessage(name + ": " + instance.get()));
    }

}
