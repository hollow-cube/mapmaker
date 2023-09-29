package net.hollowcube.terraform.command.lib;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class Command {

    protected Command(@NotNull String name) {

    }

    protected void addSyntax(@NotNull BiConsumer<Player, Context> executor, @NotNull Argument<?>... args) {

    }

    public interface Context {

    }

}
