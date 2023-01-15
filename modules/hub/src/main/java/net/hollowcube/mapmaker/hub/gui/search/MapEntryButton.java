package net.hollowcube.mapmaker.hub.gui.search;

import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.mapmaker.model.MapData;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class MapEntryButton extends ButtonSection {
    private final MapData map;
    private final boolean clickToView;

    /**
     * Creates a button representing a map, used for showing maps in long lists.
     *
     * @param clickToView If true, clicking the button will show the {@link PlayMapView} for the map.
     */
    public MapEntryButton(@NotNull MapData map, boolean clickToView) {
        super(1, 1, ItemStack.AIR);
        this.map = map;
        this.clickToView = clickToView;

        setItem(buildItemStack());
        setOnClick(this::handleClick);
    }

    private void handleClick() {
        if (!clickToView) return;

        var router = find(RouterSection.class);
        //todo push play map view
        router.push(new PlayMapView(map));
    }

    private @NotNull ItemStack buildItemStack() {
        //todo
        return ItemStack.of(Material.ENCHANTING_TABLE)
                .withDisplayName(Component.text(map.getName()));
    }
}
