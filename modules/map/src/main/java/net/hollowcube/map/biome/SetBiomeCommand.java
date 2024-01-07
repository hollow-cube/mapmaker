package net.hollowcube.map.biome;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.argold.Argument;
import net.hollowcube.command.arg.Argument2;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.map.MapFeatureFlags;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.ChunkHack;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.map.util.MapCondition.mapFeature;
import static net.hollowcube.map.util.MapCondition.mapFilter;

public class SetBiomeCommand extends CommandDsl {
    private final Argument2<Biome> biomeArg = Argument2.Word("biome")
            .map(
                    (sender, value) -> {
                        value = value.toLowerCase(Locale.ROOT);
                        if (!(sender instanceof Player player)) {
                            return new Argument.ParseFailure<>();
                        }

                        var biomeContainer = MapWorld.forPlayer(player).biomes();
                        for (var biome : biomeContainer.loadedBiomes()) {
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

                        var biomeContainer = MapWorld.forPlayer(player).biomes();
                        for (var biome : biomeContainer.loadedBiomes()) {
                            var nsid = biome.name();
                            if (nsid.asString().startsWith(value) || nsid.path().startsWith(value)) {
                                suggestion.add(nsid.asString());
                            }
                        }
                    }
            );

    @Inject
    public SetBiomeCommand() {
        super("setbiome");

        setCondition(and(
                mapFilter(false, true, false),
                mapFeature(MapFeatureFlags.BIOME_EDITOR)
        ));

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


//        var chunks = new HashSet<Chunk>();
//
//        var instance = player.getInstance();
//        for (var block : region) {
//            int cx = block.blockX() << 4, cz = block.blockZ() << 4;
//            var chunk = instance.getChunk(cx, cz);
//            if (chunk == null || !chunk.isLoaded()) continue; // Unloaded chunk
//            chunk.setBiome(block.sub(cx * 16, 0, cz * 16), biome);
//            System.out.println(chunk.getBiome(block.sub(cx * 16, 0, cz * 16)));
//        }

        var chunk = player.getInstance().getChunkAt(player.getPosition());
        var sec = chunk.getSectionAt(player.getPosition().blockY());
        sec.biomePalette().fill(biome.id());

//        for (var chunk : chunks) {
        ChunkHack.invalidateChunk(chunk);
        chunk.sendChunk();
//        }

        player.sendMessage("done!");
    }
}
