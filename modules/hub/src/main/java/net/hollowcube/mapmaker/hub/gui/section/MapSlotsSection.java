package net.hollowcube.mapmaker.hub.gui.section;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.Section;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.mapmaker.lang.LanguageProvider;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.util.StaticAbuse;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class MapSlotsSection extends ParentSection {

    public MapSlotsSection(@NotNull PlayerData playerData) {
        super(5, 1);

        for (int i = 0; i < PlayerData.MAX_MAP_SLOTS; i++) {
            var slot = i + 1;
            if (i >= playerData.getUnlockedMapSlots()) {
                add(i, createLockedSlotButton(slot));
                continue;
            }

            var mapId = playerData.getMapSlot(i);
            if (mapId != null) {
                var mapFuture = StaticAbuse.mapStorage.getMapById(mapId).map(data -> {
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return data;
                });
                add(i, new MapSlotButton(slot, mapFuture, () -> {
                    var router = find(RouterSection.class);
                    router.push(new MapSlotView(slot, mapFuture));
                }));
            } else {
                add(i, createEmptySlotIcon(slot));
            }
        }
    }

    private @NotNull Section createLockedSlotButton(int slot) {
        var item = ItemStack.builder(Material.BEDROCK)
                .amount(slot)
                .displayName(Component.translatable("gui.map_slot.locked.name", Component.text(slot + 1)))
                .lore(LanguageProvider.createMultiTranslatable("gui.map_slot.locked.lore", Component.text(slot + 1)))
                .build();
        return new ButtonSection(1, 1, item, () -> {/* todo */});
    }

    private @NotNull Section createEmptySlotIcon(int slot) {
        var item = ItemStack.builder(Material.RED_CONCRETE)
                .amount(slot)
                .displayName(Component.translatable("gui.map_slot.empty.name", Component.text(slot + 1)))
                .lore(LanguageProvider.createMultiTranslatable("gui.map_slot.empty.lore", Component.text(slot + 1)))
                .build();
        return new ButtonSection(1, 1, item, () -> {/* todo */});
    }

}
