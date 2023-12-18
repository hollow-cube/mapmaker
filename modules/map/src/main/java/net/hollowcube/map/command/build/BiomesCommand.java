package net.hollowcube.map.command.build;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.map.gui.biome.BiomeListView;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.util.MapCondition.mapFilter;

public class BiomesCommand extends Command {

    public BiomesCommand() {
        super("biomes");

        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::openCustomBiomeList));
    }

    private void openCustomBiomeList(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);
        world.server().newOpenGUI(player, c -> new BiomeListView(c, world.biomes()));
    }

}
