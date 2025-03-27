package net.hollowcube.mapmaker.gui.play.collection.list;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.play.collection.MapCollectionView;
import net.hollowcube.mapmaker.map.MapCollection;
import net.hollowcube.mapmaker.util.ItemUtils;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MapCollectionEntry extends View {


    private @Outlet("label") Label label;
    private final @NotNull MapCollection collection;

    public MapCollectionEntry(@NotNull Context context, @NotNull MapCollection collection) {
        super(context);

        this.collection = collection;

        this.label.setItemSprite(ItemUtils.asDisplay(Objects.requireNonNullElse(collection.icon(), Material.BARRIER)));
    }

    @Action("label")
    private void handleSelect() {
        this.pushView(context -> new MapCollectionView(context, this.collection));
    }


}
