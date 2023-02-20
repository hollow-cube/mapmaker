package net.hollowcube.terraform.command.argument;

import net.hollowcube.terraform.mask.script.MaskParseException;
import net.hollowcube.terraform.mask.script.MaybeMask;
import net.hollowcube.terraform.mask.script.Parser;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ExtraArguments {
    private ExtraArguments() {}

    public static @NotNull Argument<String> Selection(@NotNull String id) {
        return ArgumentType.String(id)
                .setSuggestionCallback((sender, context, suggestion) -> {
                    if (!(sender instanceof Player player)) {
                        // Never suggest for non-players
                        return;
                    }

                    var session = LocalSession.forPlayer(player);
                    for (var selectionName : session.selectionNames()) {
                        //todo add a hover giving some information about the selections.
                        suggestion.addEntry(new SuggestionEntry(selectionName));
                    }
                });
    }

    //todo need to fix ArgumentSyntaxException
    //todo maybemask is a result of the fact that map gets called even for suggestions, not just when trying to execute the command... for some reason...
    public static @NotNull Argument<@NotNull MaybeMask> Mask(@NotNull String id) {
        return ArgumentType.String(id)
                .setSuggestionCallback((sender, context, suggestion) -> {
                    suggestion.addEntry(new SuggestionEntry("|", Component.text("NOT operator, todo doc string\nbla", NamedTextColor.RED)
                            .append(Component.newline()).append(Component.text("Another blah"))));
                })
                .map(str -> {
                    try {
                        var tree = new Parser(str).parse();
                        return new MaybeMask.Mask(tree.toMask());
                    } catch (MaskParseException e) {
                        //todo really parse should never throw mask parse exception
                        return new MaybeMask.Error(e);
                    }
                });
    }
}
