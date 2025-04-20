package net.hollowcube.mapmaker.gui.play.collection.list;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.play.collection.MapCollectionView;
import net.hollowcube.mapmaker.map.MapCollection;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.util.ItemUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MapCollectionEntry extends View {

    protected @ContextObject MapService mapService;

    protected @Outlet("label") Label label;
    protected final @NotNull MapCollection collection;

    public MapCollectionEntry(@NotNull Context context, @NotNull MapCollection collection, @NotNull Component playerName) {
        super(context);

        this.collection = collection;

        this.label.setItemSprite(ItemUtils.asDisplay(Objects.requireNonNullElse(collection.icon(), Material.BARRIER)));
        this.label.setArgs(
                Component.text(Objects.requireNonNullElse(this.collection.name(), "Unnamed Collection")),
                playerName,
                Component.text(this.collection.mapIds().size())
        );
    }

    @Action(value = "label", async = true)
    private void handleSelect(Player player, int slot, ClickType click) {
        onClick(player, click);
    }

    protected void onClick(Player player, ClickType click) {
        if (Objects.requireNonNull(click) == ClickType.LEFT_CLICK) {
            this.pushView(context -> new MapCollectionView(context, this.collection));
        }
    }

}
