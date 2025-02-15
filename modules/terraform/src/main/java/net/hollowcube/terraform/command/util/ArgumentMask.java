package net.hollowcube.terraform.command.util;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.mask.script.MaskParseException;
import net.hollowcube.terraform.mask.script.MaskParser;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class ArgumentMask extends Argument<Mask> {

    ArgumentMask(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Mask> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var raw = reader.readWord(WordType.GREEDY);
        try {
            var tree = new MaskParser(raw).parse();
            if (tree == null) return partial();
            return success(tree.toMask());
        } catch (MaskParseException e) {
            return partial(e.getMessage());
        }
    }
}
