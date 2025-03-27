package net.hollowcube.mapmaker.gui.play.collection;

import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.play.MapEntry;
import net.hollowcube.mapmaker.map.MapCollection;
import net.hollowcube.mapmaker.map.MapData;
import org.jetbrains.annotations.NotNull;

public class MapCollectionView extends BaseMapCollectionView<MapEntry> {

    public MapCollectionView(@NotNull Context context, @NotNull MapCollection collection) {
        super(context, collection);
    }

    public MapCollectionView(@NotNull Context context, @NotNull String id) {
        super(context, id);
    }

    @Override
    protected MapEntry createEntry(@NotNull Context context, @NotNull MapData data) {
        return new MapEntry(context, data);
    }
}
