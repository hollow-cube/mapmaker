package net.hollowcube.map.command.build;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.map.MapFeatureFlags;
import net.hollowcube.map.gui.biome.BiomeListView;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.map.util.MapCondition.mapFeature;
import static net.hollowcube.map.util.MapCondition.mapFilter;

public class BiomesCommand extends CommandDsl {

    @Inject
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
        world.server().newOpenGUI(player, c -> new BiomeListView(c, world.biomes()));
    }

}
