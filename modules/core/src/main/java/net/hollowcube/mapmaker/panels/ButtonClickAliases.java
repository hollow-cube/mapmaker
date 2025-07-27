package net.hollowcube.mapmaker.panels;

import org.jetbrains.annotations.NotNull;

// This is gross so i wanted to hide it away.
interface ButtonClickAliases {

    default @NotNull Button onLeftClick(Button.OnClick onClick) {
        return onLeftClick((_, _) -> onClick.onClick());
    }

    default @NotNull Button onLeftClickAsync(Button.OnClick onClick) {
        return onLeftClickAsync((_, _) -> onClick.onClick());
    }

    default @NotNull Button onLeftClick(Button.OnClickType onClick) {
        return onLeftClick((clickType, _) -> onClick.onClick(clickType));
    }

    default @NotNull Button onLeftClickAsync(Button.OnClickType onClick) {
        return onLeftClickAsync((clickType, _) -> onClick.onClick(clickType));
    }

    @NotNull Button onLeftClick(Button.OnClickTypeSlot onClick);

    @NotNull Button onLeftClickAsync(Button.OnClickTypeSlot onClick);


    default @NotNull Button onRightClick(Button.OnClick onClick) {
        return onRightClick((_, _) -> onClick.onClick());
    }

    default @NotNull Button onRightClickAsync(Button.OnClick onClick) {
        return onRightClickAsync((_, _) -> onClick.onClick());
    }

    default @NotNull Button onRightClick(Button.OnClickType onClick) {
        return onRightClick((clickType, _) -> onClick.onClick(clickType));
    }

    default @NotNull Button onRightClickAsync(Button.OnClickType onClick) {
        return onRightClickAsync((clickType, _) -> onClick.onClick(clickType));
    }

    @NotNull Button onRightClick(Button.OnClickTypeSlot onClick);

    @NotNull Button onRightClickAsync(Button.OnClickTypeSlot onClick);


    default @NotNull Button onShiftLeftClick(Button.OnClick onClick) {
        return onShiftLeftClick((_, _) -> onClick.onClick());
    }

    default @NotNull Button onShiftLeftClickAsync(Button.OnClick onClick) {
        return onShiftLeftClickAsync((_, _) -> onClick.onClick());
    }

    default @NotNull Button onShiftLeftClick(Button.OnClickType onClick) {
        return onShiftLeftClick((clickType, _) -> onClick.onClick(clickType));
    }

    default @NotNull Button onShiftLeftClickAsync(Button.OnClickType onClick) {
        return onShiftLeftClickAsync((clickType, _) -> onClick.onClick(clickType));
    }

    @NotNull Button onShiftLeftClick(Button.OnClickTypeSlot onClick);

    @NotNull Button onShiftLeftClickAsync(Button.OnClickTypeSlot onClick);


}
