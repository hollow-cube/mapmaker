package net.hollowcube.terraform.command.util;

import net.hollowcube.command.arg.Argument2;
import net.hollowcube.command.argold.Argument;
import net.hollowcube.command.util.WordType;
import net.hollowcube.terraform.Terraform;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.Clipboard;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class TFArgument {

    public static @NotNull Argument2<Selection> Selection(@NotNull String id) {
        return Argument2.Word(id).map(
                /* Mapper */ (sender, raw) -> {
                    if (!(sender instanceof Player player))
                        return new Argument.ParseFailure<>();
                    raw = raw.toLowerCase(Locale.ROOT);

                    var session = LocalSession.forPlayer(player);
                    var partial = false;
                    for (var selectionName : session.selectionNames()) {
                        if (selectionName.equalsIgnoreCase(raw))
                            return new Argument.ParseSuccess<>(session.selection(selectionName));
                        if (selectionName.startsWith(raw)) partial = true;
                    }
                    return partial ? new Argument.ParsePartial<>() : new Argument.ParseFailure<>();
                },
                /* Suggestor */ (sender, reader, suggestion, raw) -> {
                    //todo in every case suggestions should only be relevant for players.
                    if (!(sender instanceof Player player)) return;

                    var word = reader.readWord(WordType.ALPHANUMERIC).toLowerCase(Locale.ROOT);
                    var session = LocalSession.forPlayer(player);
                    for (var selectionName : session.selectionNames()) {
                        if (selectionName.startsWith(word)) {
                            suggestion.add(selectionName);
                        }
                    }
                }
        ).defaultValue(sender -> {
            if (!(sender instanceof Player player)) return null;
            return LocalSession.forPlayer(player).selection(Selection.DEFAULT);
        }).errorHandler((sender, unused) -> {
            sender.sendMessage("no such clipboard: todo add name here");
        });
    }

    public static @NotNull Argument2<Clipboard> Clipboard(@NotNull String id) {
        return Argument2.Word(id).map(
                /* Mapper */ (sender, raw) -> {
                    if (!(sender instanceof Player player))
                        return new Argument.ParseFailure<>();
                    raw = raw.toLowerCase(Locale.ROOT);

                    var session = PlayerSession.forPlayer(player);
                    var partial = false;
                    for (var selectionName : session.clipboardNames()) {
                        if (selectionName.equalsIgnoreCase(raw))
                            return new Argument.ParseSuccess<>(session.clipboard(selectionName));
                        if (selectionName.startsWith(raw)) partial = true;
                    }
                    return partial ? new Argument.ParsePartial<>() : new Argument.ParseFailure<>();
                },
                /* Suggestor */ (sender, reader, suggestion, raw) -> {
                    //todo in every case suggestions should only be relevant for players.
                    if (!(sender instanceof Player player)) return;

                    var word = reader.readWord(WordType.ALPHANUMERIC).toLowerCase(Locale.ROOT);
                    var session = PlayerSession.forPlayer(player);
                    for (var selectionName : session.clipboardNames()) {
                        if (selectionName.startsWith(word)) {
                            suggestion.add(selectionName);
                        }
                    }
                }
        ).defaultValue(sender -> {
            if (!(sender instanceof Player player)) return null;
            return PlayerSession.forPlayer(player).clipboard(Selection.DEFAULT);
        });
    }

    public static @NotNull Argument2<Pattern> Pattern(@NotNull String id, @NotNull Terraform tf) {
        return new ArgumentPattern(id, tf);
    }

    public static @NotNull Argument2<Mask> Mask(@NotNull String id) {
        return new ArgumentMask(id);
    }

    private TFArgument() {
    }
}
