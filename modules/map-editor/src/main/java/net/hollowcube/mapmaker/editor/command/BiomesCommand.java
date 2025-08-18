package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.editor.gui.biome.BiomeListView;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.entity.Player;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;
import static net.hollowcube.mapmaker.map.command.MapConditions.mapFeature;

public class BiomesCommand extends CommandDsl {

    public BiomesCommand() {
        super("biomes");

        setCondition(and(builderOnly(), mapFeature(MapFeatureFlags.BIOME_EDITOR)));

        addSyntax(playerOnly(this::openCustomBiomeList));
    }

    private void openCustomBiomeList(Player player, CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if (world == null) return;

        world.server().showView(player, c -> new BiomeListView(c, world.biomes()));
    }

}
