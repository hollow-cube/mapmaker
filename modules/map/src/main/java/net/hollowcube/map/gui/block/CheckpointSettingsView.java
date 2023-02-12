package net.hollowcube.map.gui.block;

import net.hollowcube.canvas.section.ClickHandler;
import net.hollowcube.canvas.section.ParentSection;
import net.hollowcube.canvas.section.std.ButtonSection;
import net.hollowcube.map.gui.common.TranslatedButtonSection;
import net.hollowcube.mapmaker.model.MapData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CheckpointSettingsView extends ParentSection {
    private final MapData.POI poi;

    public CheckpointSettingsView(@NotNull MapData.POI poi) {
        super(9, 3);
        this.poi = poi;

        add(1, 0, new ResetHeightSetting());
    }

    private class ResetHeightSetting extends ParentSection {
        private final ButtonSection indicator;

        public ResetHeightSetting() {
            super(7, 1);

            indicator = add(0, 0, new ButtonSection(1, 1, getIndicatorItem(), this::toggleActive));

            add(2, 0, new TranslatedButtonSection("gui.checkpoint.reset_height.min", List.of(), Material.RED_CONCRETE, this::setToMin));
            add(3, 0, new TranslatedButtonSection("gui.checkpoint.reset_height.step_down", List.of(), Material.RED_CONCRETE, this::stepDown));
            add(4, 0, new TranslatedButtonSection("gui.checkpoint.reset_height.reset", List.of(), Material.GRAY_CONCRETE, this::setToDefault));
            add(5, 0, new TranslatedButtonSection("gui.checkpoint.reset_height.step_up", List.of(), Material.RED_CONCRETE, this::stepUp));
            add(6, 0, new TranslatedButtonSection("gui.checkpoint.reset_height.max", List.of(), Material.GREEN_CONCRETE, this::setToMax));
        }

        private void updateIndicator() {
            indicator.setItem(getIndicatorItem());
        }

        private void toggleActive() {
            poi.set("active", !poi.getOrDefault("active", false));
            updateIndicator();
        }

        private int getResetHeight() {
            return poi.getOrDefault("resetHeight", poi.getPos().blockY() - 5);
        }

        private void setResetHeight(int value) {
            poi.set("resetHeight", value);
            updateIndicator();
        }

        private void setToMin() {
            System.out.println("Not implemented");
        }

        private boolean stepDown(@NotNull Player player, int slot, @NotNull ClickType clickType) {
            if (clickType == ClickType.LEFT_CLICK) {
                setResetHeight(getResetHeight() - 1);
            } else if (clickType == ClickType.SHIFT_CLICK) {
                setResetHeight(getResetHeight() - 5);
            }
            return ClickHandler.DENY;
        }

        private void setToDefault() {
            setResetHeight(poi.getPos().blockY() - 5);
        }

        private boolean stepUp(@NotNull Player player, int slot, @NotNull ClickType clickType) {
            if (clickType == ClickType.LEFT_CLICK) {
                setResetHeight(getResetHeight() + 1);
            } else if (clickType == ClickType.SHIFT_CLICK) {
                setResetHeight(getResetHeight() + 5);
            }
            return ClickHandler.DENY;
        }

        private void setToMax() {
            System.out.println("Not implemented");
        }

        private ItemStack getIndicatorItem() {
            return ItemStack.builder(poi.getOrDefault("active", false) ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                    .displayName(Component.text("Reset Height"))
                    .lore(Component.text("current: " + getResetHeight()), Component.text("active: " + poi.getOrDefault("active", false)))
                    .build();
        }
    }
}
