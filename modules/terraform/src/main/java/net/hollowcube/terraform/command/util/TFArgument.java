package net.hollowcube.terraform.command.util;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.SuggestionResult;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TFArgument {

    public static @NotNull Argument<Selection> Selection(@NotNull String id) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> {
                    if (!(sender instanceof Player player))
                        return new Argument.ParseFailure<>();
                    var session = LocalSession.forPlayer(player);
                    return new Argument.ParseSuccess<>(session.selection(Selection.DEFAULT));
                },
                /* Suggestor */ (sender, reader, raw) -> {
                    return new SuggestionResult.Success(0, 0, List.of());
                }
        ).defaultValue(sender -> {
            if (!(sender instanceof Player player)) return null;
            return LocalSession.forPlayer(player).selection(Selection.DEFAULT);
        });
    }

    public static @NotNull Argument<Pattern> Pattern(@NotNull String id) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> {
                    return new Argument.ParseFailure<>();
                },
                /* Suggestor */ (sender, reader, raw) -> {
                    return new SuggestionResult.Success(0, 0, List.of());
                }
        );
    }

    public static @NotNull Argument<Mask> Mask(@NotNull String id) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> {
                    return new Argument.ParseFailure<>();
                },
                /* Suggestor */ (sender, reader, raw) -> {
                    return new SuggestionResult.Success(0, 0, List.of());
                }
        );
    }

    private TFArgument() {
    }
}
