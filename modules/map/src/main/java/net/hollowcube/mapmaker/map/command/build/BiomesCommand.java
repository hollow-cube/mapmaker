package net.hollowcube.mapmaker.map.command.build;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.gui.biome.BiomeListView;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.mapmaker.map.util.MapCondition.mapFeature;
import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class BiomesCommand extends CommandDsl {

    public BiomesCommand() {
        super("biomes");

        setCondition(and(
                mapFilter(false, true, false),
                mapFeature(MapFeatureFlags.BIOME_EDITOR)
        ));

        addSyntax(playerOnly(this::openCustomBiomeList));
    }

    private void openCustomBiomeList(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);
        world.server().showView(player, c -> new BiomeListView(c, world.biomes()));
    }

}
