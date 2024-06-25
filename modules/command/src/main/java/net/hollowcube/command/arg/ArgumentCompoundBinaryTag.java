package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArgumentCompoundBinaryTag extends Argument<CompoundBinaryTag> {

    ArgumentCompoundBinaryTag(@NotNull String id) {
        super(id);
    }

    @Override
    public @Nullable String vanillaParser() {
        return "nbt_compound_tag";
    }

    @Override
    public @NotNull ParseResult<CompoundBinaryTag> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        final String word = reader.readWord(WordType.GREEDY);
        try {
            return success(TagStringIO.get().asCompound(word));
        } catch (Exception e) {
            return syntaxError();
        }
    }

}
