package net.hollowcube.terraform.compat.worldedit.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.terraform.instance.TerraformInstanceBiomes;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ArgumentBiome extends Argument<DynamicRegistry.Key<Biome>> {

    ArgumentBiome(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<DynamicRegistry.Key<Biome>> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        if (!(sender instanceof Player player)) return syntaxError();

        var biomes = TerraformInstanceBiomes.forInstance(player.getInstance());
        var raw = reader.readWord(WordType.BRIGADIER).toLowerCase(Locale.ROOT);

        if (biomes == null || raw.isEmpty()) return partial();

        boolean partial = false;

        for (DynamicRegistry.Key<Biome> key : biomes.keys()) {
            var miniId = key.key().asMinimalString().toLowerCase(Locale.ROOT);
            var id = key.key().asString().toLowerCase(Locale.ROOT);

            if (id.equals(raw) || miniId.equals(raw)) return new ParseResult.Success<>(key);
            if (id.startsWith(raw) || miniId.startsWith(raw)) partial = true;
        }

        return partial ? partial() : syntaxError();
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        if (!(sender instanceof Player player)) return;

        var biomes = TerraformInstanceBiomes.forInstance(player.getInstance());
        raw = raw.toLowerCase(Locale.ROOT);

        if (biomes == null) return;

        for (DynamicRegistry.Key<Biome> key : biomes.keys()) {
            var miniId = key.key().asMinimalString().toLowerCase(Locale.ROOT);
            var id = key.key().asString().toLowerCase(Locale.ROOT);

            if (!raw.isEmpty() && miniId.startsWith(raw)) {
                suggestion.add(id);
            } else if (id.startsWith(raw)) {
                suggestion.add(miniId);
            }
        }
    }
}
