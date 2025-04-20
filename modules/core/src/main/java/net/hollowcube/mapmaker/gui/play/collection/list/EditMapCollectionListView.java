package net.hollowcube.mapmaker.gui.play.collection.list;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.MapCollection;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditMapCollectionListView extends MapCollectionListView {

    public EditMapCollectionListView(@NotNull Context context) {
        super(context);
    }

    @Override
    protected @Nullable MapCollectionEntry createEntry(@NotNull Context context, @NotNull MapCollection collection, @NotNull Component playername) {
        return new EditMapCollectionEntry(context, collection, playername);
    }

    @Signal(View.SIG_MOUNT)
    public void onMount() {
        this.collections = null;
    }
}
