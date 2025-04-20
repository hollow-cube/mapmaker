package net.hollowcube.mapmaker.gui.play.collection.list;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.play.collection.edit.EditMapCollectionView;
import net.hollowcube.mapmaker.map.MapCollection;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AddMapCollectionEntry extends MapCollectionEntry {

    private final String mapId;

    public AddMapCollectionEntry(@NotNull Context context, @NotNull MapCollection collection, @NotNull Component playerName, String mapId) {
        super(context, collection, playerName);
        this.mapId = mapId;
    }

    @Override
    protected void onClick(Player player, ClickType click) {
        if (Objects.requireNonNull(click) != ClickType.LEFT_CLICK) return;

        this.collection.mapIds().addFirst(this.mapId);
        this.mapService.updateMapCollection(player.getUuid().toString(), this.collection);
        this.pushView(context -> new EditMapCollectionView(context, this.collection));
    }
}
