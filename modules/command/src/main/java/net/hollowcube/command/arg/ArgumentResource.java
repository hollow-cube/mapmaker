package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.kyori.adventure.key.Keyed;
import net.minestom.server.command.CommandSender;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class ArgumentResource<T extends Keyed> extends Argument<T> {

    private final String registryName;
    private final Function<NamespaceID, T> mapper;

    ArgumentResource(@NotNull String id, @NotNull String registryName, @NotNull Function<NamespaceID, T> mapper) {
        super(id);
        this.registryName = registryName;
        this.mapper = mapper;
    }

    @Override
    public @NotNull ParseResult<T> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        final String word = reader.readWord(WordType.GREEDY);
        try {
            final NamespaceID namespace = NamespaceID.from(word);
            final T resource = mapper.apply(namespace);
            if (resource == null) return syntaxError();
            return success(resource);
        } catch (Exception e) {
            return syntaxError();
        }
    }

}
