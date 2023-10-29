package net.hollowcube.terraform.give_me_new_home.helper;

import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ExtraArguments {
    private ExtraArguments() {
    }

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
}
