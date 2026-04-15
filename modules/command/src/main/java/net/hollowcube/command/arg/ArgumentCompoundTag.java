package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.common.parsing.ParsingException;
import net.hollowcube.common.parsing.snbt.Snbt;
import net.hollowcube.common.parsing.snbt.SnbtReader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public class ArgumentCompoundTag extends Argument<CompoundBinaryTag> {

    private static final int COMMAND_SNBT_MAX_DEPTH = 16;

    ArgumentCompoundTag(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<CompoundBinaryTag> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var remainingText = reader.raw().substring(reader.pos());
        var snbtReader = new SnbtReader(remainingText, COMMAND_SNBT_MAX_DEPTH);
        try {
            var tag = Snbt.parse(snbtReader);
            if (!(tag instanceof CompoundBinaryTag compoundTag)) {
                return new ParseResult.Failure<>(reader.pos(), "Expected a compound tag");
            }
            return new ParseResult.Success<>(compoundTag);
        } catch (ParsingException exception) {
            return new ParseResult.Failure<>(
                reader.pos() + exception.cursor(),
                "Failed to parse tag, " + exception.getMessage()
            );
        } finally {
            reader.restore(reader.pos() + snbtReader.cursor());
        }
    }

    @Override
    public @NotNull ArgumentParserType argumentType() {
        return ArgumentParserType.NBT_COMPOUND_TAG;
    }

    @Override
    public void properties(@NotNull NetworkBuffer buffer) {
    }

    @Override
    public boolean shouldSuggest() {
        return false;
    }
}
