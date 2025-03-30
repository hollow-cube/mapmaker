package net.hollowcube.mapmaker.command.map;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.play.collection.MapCollectionView;
import net.hollowcube.mapmaker.gui.play.collection.list.EditMapCollectionListView;
import net.hollowcube.mapmaker.map.MapService;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Notably not using {@link net.hollowcube.command.dsl.CommandDsl}, it doesn't support arguments followed by "subcommands" very well.
 */
public class MapCollectionCommand extends CommandDsl {

    private final Argument<UUID> idArg = Argument.UUID("id")
            .description("The ID of the collection to view")
            .defaultValue((UUID) null);

    private final Controller controller;
    private final MapService maps;

    public MapCollectionCommand(@NotNull Controller controller, @NotNull MapService maps) {
        super("collection");
        this.controller = controller;
        this.maps = maps;

        addSyntax(playerOnly(this::handleViewCollections));
        addSyntax(playerOnly(this::handleViewCollection), idArg);
    }

    private void handleViewCollections(@NotNull Player player, @NotNull CommandContext ctx) {
        this.controller.show(player, EditMapCollectionListView::new);
    }

    private void handleViewCollection(@NotNull Player player, @NotNull CommandContext ctx) {
        var collectionId = ctx.get(idArg);
        if (collectionId == null) {
            player.sendMessage("No collection ID specified");
            return;
        }

        try {
            var collection = this.maps.getMapCollection(player.getUuid().toString(), collectionId.toString());
            this.controller.show(player, c -> new MapCollectionView(c, collection));
        } catch (MapService.NotFoundError e) {
            player.sendMessage("No collection found for ID " + collectionId);
        } catch (Exception e) {
            player.sendMessage("Failed to load collection");
            ExceptionReporter.reportException(e, player);
        }
    }

}
