package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.kyori.adventure.key.Key;
import net.minestom.server.command.CommandSender;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ArgumentMaterial extends Argument<Material> {
    private static final Set<Key> MATERIAL_IDS = Material.values().stream()
            .map(Material::key).collect(Collectors.toUnmodifiableSet());

    ArgumentMaterial(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Material> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var word = reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT);
        if (!Key.parseable(word))
            return syntaxError("not a material: " + word);
        var material = Material.fromKey(word);
        if (material != null) return success(material);

        // Not an exact match so just need to get to a single partial match.
        for (var materialId : MATERIAL_IDS) {
            if (materialId.asString().startsWith(word) || materialId.value().startsWith(word))
                return partial();
        }

        return syntaxError();
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        raw = raw.toLowerCase(Locale.ROOT);
        for (var materialId : MATERIAL_IDS) {
            if (materialId.asString().startsWith(raw) || materialId.value().startsWith(raw))
                suggestion.add(materialId.asString());
        }
    }
}
