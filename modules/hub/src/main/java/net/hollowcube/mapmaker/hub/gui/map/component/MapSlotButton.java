package net.hollowcube.mapmaker.hub.gui.map.component;

import net.hollowcube.canvas.section.std.ButtonSection;
import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.model.MapData;
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

    private final int rawSlot;

    public MapSlotButton(int rawSlot, @NotNull FutureResult<MapData> mapFuture) {
        this(rawSlot, mapFuture, null);
    }

    public MapSlotButton(int rawSlot, @NotNull FutureResult<MapData> mapFuture, @Nullable Runnable onClick) {
        super(1, 1, LOADING_ICON, onClick);
        this.rawSlot = rawSlot;

        mapFuture.then(this::mapLoaded).thenErr(this::mapLoadError);
    }

    private void mapLoaded(@NotNull MapData mapData) {
        var args = List.of(Component.text(rawSlot + 1),
                Component.text(mapData.getOwner()),
                Component.text(mapData.getName()),
                Component.text(mapData.getId()));
        setItem(ItemStack.builder(Material.GREEN_CONCRETE)
                .amount(rawSlot + 1)
                .displayName(Component.translatable("gui.map_slot.map.name", args))
                .lore(LanguageProvider.createMultiTranslatable("gui.map_slot.map.lore", args.toArray(new Component[0])))
                .build());
    }

    private void mapLoadError(@NotNull Error err) {
        //todo translations
        setItem(ItemStack.builder(Material.BARRIER)
                .amount(rawSlot)
                .displayName(Component.text(err.message()))
                .build());
    }
}
