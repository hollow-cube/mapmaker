package net.hollowcube.mapmaker.gui.play.collection;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.play.MapEntry;
import net.hollowcube.mapmaker.map.MapCollection;
import net.hollowcube.mapmaker.map.MapData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MapCollectionView extends BaseMapCollectionView<MapEntry> {

    protected @Outlet("name") Text name;

    public MapCollectionView(@NotNull Context context, @NotNull MapCollection collection) {
        super(context, collection);
    }

    public MapCollectionView(@NotNull Context context, @NotNull String id) {
        super(context, id);
    }

    @Override
    protected void onLoaded(@NotNull MapCollection collection) {
        this.name.setText(Objects.requireNonNullElse(collection.name(), "Unnamed Collection"));
        this.name.setArgs(Objects.requireNonNullElse(this.ownerName, Component.text("Unknown").color(NamedTextColor.RED)));
    }

    @Override
    protected MapEntry createEntry(@NotNull Context context, @NotNull MapData data) {
        return new MapEntry(context, data);
    }
}
