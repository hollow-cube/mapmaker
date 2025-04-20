package net.hollowcube.mapmaker.gui.play.collection.list;

import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.common.anvil.TextInputView;
import net.hollowcube.mapmaker.map.MapCollection;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class AddMapCollectionListView extends MapCollectionListView {

    private final String mapId;

    public AddMapCollectionListView(@NotNull Context context, String mapId) {
        super(context);
        this.mapId = mapId;
    }

    @Override
    protected @Nullable MapCollectionEntry createEntry(@NotNull Context context, @NotNull MapCollection collection, @NotNull Component playername) {
        if (collection.mapIds().contains(this.mapId)) return null;
        return new AddMapCollectionEntry(context, collection, playername, this.mapId);
    }

    @Action("add")
    public void handleAdd(@NotNull Player player) {
        this.pushTransientView(context -> TextInputView.builder()
                .title("Create Map Collection")
                .callback(($1, text) -> {
                    this.mapService.createMapCollection(
                            player.getUuid().toString(),
                            text,
                            Material.PAPER.name(),
                            new ArrayList<>()
                    );
                    this.pagination.reset();
                    this.pushView(new AddMapCollectionListView(context, this.mapId));
                })
                .build(context)
        );
    }
}
