package net.hollowcube.terraform.command.helper;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;

public class ArgumentSwizzle extends Argument<Byte> {
    public static final byte X = 0x1;
    public static final byte Y = 0x2;
    public static final byte Z = 0x4;

    public ArgumentSwizzle(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull Byte parse(@NotNull CommandSender sender, @NotNull String input) throws ArgumentSyntaxException {
        if (input.isEmpty()) throw new ArgumentSyntaxException("Expected swizzle", input, 0);

        byte result = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                //todo this is more lenient than minecraft, it will handle duplicates. Probably should not.
                case 'x' -> result |= X;
                case 'y' -> result |= Y;
                case 'z' -> result |= Z;
                default -> throw new ArgumentSyntaxException("Expected x, y, or z", input, i);
            }
        }

        return result;
    }

    @Override
    public String parser() {
        return "minecraft:swizzle";
    }
}
