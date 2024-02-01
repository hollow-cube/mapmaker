package net.hollowcube.terraform.command.util;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class ArgumentPattern extends Argument<Pattern> {
    private static final List<NamespaceID> BLOCKS = Block.values().stream()
            .map(Block::namespace).sorted().toList();

    ArgumentPattern(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Pattern> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        if (!(sender instanceof Player player)) {
            return syntaxError();
        }

        var word = reader.readWord(WordType.GREEDY);
        try {
            var rawBlockState = ArgumentBlockState.staticParse(word);

            // Remap the block state using the registry, todo just rework this whole thing to use tf registry to start.
            var tf = LocalSession.forPlayer(player).terraform();
            var blockState = tf.registry().blockState(rawBlockState.stateId());

            return success((world, blockPosition) -> blockState);
        } catch (ArgumentSyntaxException e) {
            return partial();
        }
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        var word = raw.toLowerCase(Locale.ROOT);
        for (var block : BLOCKS) {
            if (block.asString().startsWith(word) || block.path().startsWith(word)) {
                suggestion.add(block.asString());
            }

            if (suggestion.getEntries().size() > 20) {
                break;
            }
        }
    }
}
