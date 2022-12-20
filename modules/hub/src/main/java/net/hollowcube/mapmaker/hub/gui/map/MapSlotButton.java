package net.hollowcube.mapmaker.hub.gui.map;

import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.mapmaker.lang.LanguageProvider;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.result.Error;
import net.hollowcube.mapmaker.result.FutureResult;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A button representing a map slot.
 * <p>
 * Used in both the slot list view and single slot view.
 */
public class MapSlotButton extends ButtonSection {
    public static final ItemStack LOADING_ICON = ItemStack.builder(Material.GREEN_CONCRETE)
            .displayName(Component.translatable("gui.map_slot.loading.name"))
            .build();

    private final int slot;

    public MapSlotButton(int slot, @NotNull FutureResult<MapData> mapFuture) {
        this(slot, mapFuture, null);
    }

    public MapSlotButton(int slot, @NotNull FutureResult<MapData> mapFuture, @Nullable Runnable onClick) {
        super(1, 1, LOADING_ICON, onClick);
        this.slot = slot;

        mapFuture.then(this::mapLoaded).thenErr(this::mapLoadError);
    }

    private void mapLoaded(@NotNull MapData mapData) {
        var args = List.of(Component.text(slot),
                Component.text(mapData.getOwner()),
                Component.text(mapData.getName()),
                Component.text(mapData.getId()));
        setItem(ItemStack.builder(Material.GREEN_CONCRETE)
                .amount(slot)
                .displayName(Component.translatable("gui.map_slot.map.name", args))
                .lore(LanguageProvider.createMultiTranslatable("gui.map_slot.map.lore", args.toArray(new Component[0])))
                .build());
    }

    private void mapLoadError(@NotNull Error err) {
        //todo translations
        setItem(ItemStack.builder(Material.BARRIER)
                .amount(slot)
                .displayName(Component.text(err.message()))
                .build());
    }
}
