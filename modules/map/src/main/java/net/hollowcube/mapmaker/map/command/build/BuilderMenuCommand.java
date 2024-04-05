package net.hollowcube.mapmaker.map.command.build;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import net.hollowcube.mapmaker.map.gui.buildermenu.BuilderMenuView;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class BuilderMenuCommand extends CommandDsl {

    @Inject
    public BuilderMenuCommand() {
        super("buildermenu", "bm");

        description = "Open the builder menu with a command instead of an item";

        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::openBuilderMenu));
    }

    private void openBuilderMenu(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity

        world.server().showView(player, BuilderMenuView::new);
    }

}
