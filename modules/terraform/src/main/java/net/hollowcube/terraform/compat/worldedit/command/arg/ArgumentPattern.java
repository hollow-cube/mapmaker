package net.hollowcube.terraform.compat.worldedit.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.terraform.compat.worldedit.script.PatternParser;
import net.hollowcube.terraform.compat.worldedit.script.PatternTree;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.script.ParseContext;
import net.hollowcube.terraform.util.script.ParseException;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

// This is notably distinct from the other ArgumentPattern. This is for WorldEdit patterns specifically.
public class ArgumentPattern extends Argument<Pattern> {

    ArgumentPattern(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Pattern> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        if (!(sender instanceof Player player)) return syntaxError();
        var session = LocalSession.forPlayer(player);

        var word = reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT);
        var tree = new PatternParser(word).parse();
        if (tree == null) {
//            System.out.println("tree: none");
            return partial();
        }

        try {
//            System.out.println("tree: " + tree);
            return success(tree.into(ParseContext.of(session.terraform().registry(), player)));
        } catch (ParseException e) {
//            System.out.println(e.getMessage());
            return partial(e.getMessage());
        }
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        if (!(sender instanceof Player player)) return;
        var session = LocalSession.forPlayer(player);
        var registry = session.terraform().registry();

        var tree = new PatternParser(raw).parse();
        if (tree == null) {
            PatternTree.fillEmptySuggestion(registry, suggestion, 0);
        } else {
            tree.suggest(registry, suggestion);
        }
    }
}
