package net.hollowcube.command.dsl;

import net.hollowcube.command.util.CommandCategory;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class SimpleCommand {

    private final String name;
    private @Nullable CommandCategory category = CommandCategory.DEFAULT;
    private @Nullable String description = null;
    private Consumer<Player> callback = player -> {};

    public SimpleCommand(String name) {
        this.name = name;
    }

    public static SimpleCommand of(String name) {
        return new SimpleCommand(name);
    }

    public SimpleCommand description(String description) {
        this.description = description;
        return this;
    }

    public SimpleCommand callback(Consumer<Player> callback) {
        this.callback = callback;
        return this;
    }

    public CommandDsl build() {
        return new CommandDsl(name) {
            {
                description = SimpleCommand.this.description;
                category = SimpleCommand.this.category;

                addSyntax(playerOnly((player, context) -> callback.accept(player)));
            }
        };
    }

}
