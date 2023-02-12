package net.hollowcube.canvas.experiment.impl;

import net.hollowcube.canvas.section.ClickHandler;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ButtonElement extends LabelElement {
    private int zIndex = 0;
    private final List<ClickHandler> handlers = new ArrayList<>();

    public ButtonElement(@Nullable String id, int width, int height,
                         @NotNull String translationKey, @NotNull Component... args) {
        super(id, width, height, translationKey, args);
    }

    @Override
    public int zIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public void addHandler(@NotNull ClickHandler handler) {
        handlers.add(handler);
    }

    @Override
    protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
        for (ClickHandler handler : handlers) {
            handler.handleClick(player, slot, clickType);
        }
        return ClickHandler.DENY;
    }
}
