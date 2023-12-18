package net.hollowcube.map.biome;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.ChunkHack;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class SetBiomeCommand extends Command {
    private final Argument<Biome> biomeArg = Argument.Word("biome")
            .map(
                    (sender, value) -> {
                        value = value.toLowerCase(Locale.ROOT);
                        if (!(sender instanceof Player player)) {
                            return new Argument.ParseFailure<>();
                        }

                        var biomeManager = MapWorld.forPlayer(player).biomeManager();
                        for (var biome : biomeManager.values()) {
                            var nsid = biome.name();
                            if (value.equals(nsid.asString()))
                                return new Argument.ParseSuccess<>(biome);
                            if (nsid.asString().startsWith(value) || nsid.path().startsWith(value))
                                return new Argument.ParsePartial<>();
                        }

                        return new Argument.ParseFailure<>();
                    },
                    (sender, reader, suggestion, value) -> {
                        value = value.toLowerCase(Locale.ROOT);
                        if (!(sender instanceof Player player)) {
                            return;
                        }
                        //todo add util function to test similarity of namespace id and string

                        var biomeManager = MapWorld.forPlayer(player).biomeManager();
                        for (var biome : biomeManager.values()) {
                            var nsid = biome.name();
                            if (nsid.asString().startsWith(value) || nsid.path().startsWith(value)) {
                                suggestion.add(nsid.asString());
                            }
                        }
                    }
            );

    public SetBiomeCommand() {
        super("setbiome");

        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::handleSetRegionToBiome), biomeArg);
    }

    private void handleSetRegionToBiome(@NotNull Player player, @NotNull CommandContext context) {
        var biome = context.get(biomeArg);
        player.sendMessage(biome.name().asString());

        var session = LocalSession.forPlayer(player);
        var selection = session.selection(Selection.DEFAULT);
        var region = selection.region();
        if (region == null) {
            player.sendMessage(Component.translatable("terraform.generic.no_selection"));
            return;
        }

        var chunks = new HashSet<Chunk>();

        var instance = player.getInstance();
        for (var block : region) {
            int cx = block.blockX() << 4, cz = block.blockZ() << 4;
            var chunk = instance.getChunk(cx, cz);
            if (chunk == null || !chunk.isLoaded()) continue; // Unloaded chunk
            chunk.setBiome(block.sub(cx * 16, 0, cz * 16), biome);
            System.out.println(chunk.getBiome(block.sub(cx * 16, 0, cz * 16)));
        }

        for (var chunk : chunks) {
            ChunkHack.invalidateChunk(chunk);
            chunk.sendChunk();
        }

        player.sendMessage("done!");
    }
}
