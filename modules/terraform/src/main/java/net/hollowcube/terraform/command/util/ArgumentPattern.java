package net.hollowcube.terraform.command.util;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.terraform.pattern.Pattern;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class ArgumentPattern extends Argument<Pattern> {
    private static final List<String> BLOCKS = Block.values().stream()
            .map(Block::name).sorted().toList();

    ArgumentPattern(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Pattern> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.GREEDY);
        try {
            var blockState = ArgumentBlockState.staticParse(word);
            return new ParseSuccess<>((world, blockPosition) -> blockState);
        } catch (ArgumentSyntaxException e) {
            return new ParsePartial<>();
        }
    }

    @Override
    public void suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull Suggestion suggestion) {
        var word = reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT);
        for (var block : BLOCKS) {
            if (block.startsWith(word)) {
                suggestion.add(block);
            }

            if (suggestion.getEntries().size() > 20) {
                break;
            }
        }
    }
}
