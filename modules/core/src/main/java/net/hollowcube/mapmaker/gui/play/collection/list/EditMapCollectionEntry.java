package net.hollowcube.mapmaker.gui.play.collection.list;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.play.collection.MapCollectionView;
import net.hollowcube.mapmaker.gui.play.collection.edit.EditMapCollectionView;
import net.hollowcube.mapmaker.map.MapCollection;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EditMapCollectionEntry extends MapCollectionEntry {

    public EditMapCollectionEntry(@NotNull Context context, @NotNull MapCollection collection, @NotNull Component playerName) {
        super(context, collection, playerName);
    }

    @Override
    protected void onClick(Player player, ClickType click) {
        switch (click) {
            case LEFT_CLICK -> this.pushView(context -> new MapCollectionView(context, this.collection));
            case SHIFT_LEFT_CLICK -> this.pushView(context -> new EditMapCollectionView(context, this.collection));
        }
    }


}
