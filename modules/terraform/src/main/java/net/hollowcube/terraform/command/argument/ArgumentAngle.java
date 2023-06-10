package net.hollowcube.terraform.command.argument;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;

public class ArgumentAngle extends Argument<Double> {
    public ArgumentAngle(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull Double parse(@NotNull CommandSender sender, @NotNull String input) throws ArgumentSyntaxException {
        System.out.println("ANGLE: " + input);
        return 0D;
    }

    @Override
    public String parser() {
        return "minecraft:angle";
    }
}
