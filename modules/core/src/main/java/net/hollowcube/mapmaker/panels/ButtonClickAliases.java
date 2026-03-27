package net.hollowcube.mapmaker.panels;

// This is gross so i wanted to hide it away.
interface ButtonClickAliases {

    default Button onLeftClick(Button.OnClick onClick) {
        return onLeftClick((_, _) -> onClick.onClick());
    }

    default Button onLeftClickAsync(Button.OnClick onClick) {
        return onLeftClickAsync((_, _) -> onClick.onClick());
    }

    default Button onLeftClick(Button.OnClickType onClick) {
        return onLeftClick((clickType, _) -> onClick.onClick(clickType));
    }

    default Button onLeftClickAsync(Button.OnClickType onClick) {
        return onLeftClickAsync((clickType, _) -> onClick.onClick(clickType));
    }

    Button onLeftClick();

    Button onLeftClick(Button.OnClickTypeSlot onClick);

    Button onLeftClickAsync(Button.OnClickTypeSlot onClick);


    default Button onRightClick(Button.OnClick onClick) {
        return onRightClick((_, _) -> onClick.onClick());
    }

    default Button onRightClickAsync(Button.OnClick onClick) {
        return onRightClickAsync((_, _) -> onClick.onClick());
    }

    default Button onRightClick(Button.OnClickType onClick) {
        return onRightClick((clickType, _) -> onClick.onClick(clickType));
    }

    default Button onRightClickAsync(Button.OnClickType onClick) {
        return onRightClickAsync((clickType, _) -> onClick.onClick(clickType));
    }

    Button onRightClick(Button.OnClickTypeSlot onClick);

    Button onRightClickAsync(Button.OnClickTypeSlot onClick);


    default Button onShiftLeftClick(Button.OnClick onClick) {
        return onShiftLeftClick((_, _) -> onClick.onClick());
    }

    default Button onShiftLeftClickAsync(Button.OnClick onClick) {
        return onShiftLeftClickAsync((_, _) -> onClick.onClick());
    }

    default Button onShiftLeftClick(Button.OnClickType onClick) {
        return onShiftLeftClick((clickType, _) -> onClick.onClick(clickType));
    }

    default Button onShiftLeftClickAsync(Button.OnClickType onClick) {
        return onShiftLeftClickAsync((clickType, _) -> onClick.onClick(clickType));
    }

    Button onShiftLeftClick(Button.OnClickTypeSlot onClick);

    Button onShiftLeftClickAsync(Button.OnClickTypeSlot onClick);


}
