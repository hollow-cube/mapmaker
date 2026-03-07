package net.hollowcube.mapmaker.panels;

import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.InventoryType;

import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractAnvilView extends Panel {

    /**
     * Simple anvil view with an input and submit. The callback is called _after_ popping the view.
     */
    public static Panel simpleAnvil(String container, String icon, String title, Consumer<String> onSubmit) {
        return simpleAnvil(container, icon, title, onSubmit, "");
    }

    public static Panel simpleAnvil(String container, String icon, String title, Consumer<String> onSubmit, String initialValue) {
        return new AbstractAnvilView(container, icon, title, initialValue, true) {
            @Override
            protected void onSubmit(String text) {
                super.onSubmit(text);
                onSubmit.accept(text);
            }
        };
    }

    private String input;

    protected Button inputButton;

    public AbstractAnvilView(String container, String icon, String title, String initialInput, boolean withSubmit) {
        super(InventoryType.ANVIL, 9, 5);
        this.input = initialInput;

        background(container, -65, -39);
        add(0, 0, new Button(null, 0, 0)
            .disableTooltip()
            .background(icon, -46, -1)); // kinda gross

        int titleWidth = FontUtil.measureTextV2(title);
        add(0, 0, new Text(null, 0, 0, title)
            .align(-(titleWidth / 2) + 30, -31));

        this.inputButton = add(0, 0, new Button("", 1, 1)
            .disableTooltip()
            .sprite("generic2/anvil/back", -32, 30)
            .onLeftClick(() -> host.popView())
            .text(Component.text(this.input), List.of()));
        if (withSubmit) {
            add(2, 0, new Button("gui.generic.empty", 1, 1)
                .disableTooltip()
                .sprite("generic2/anvil/checkmark", 35, 29)
                .onLeftClick(() -> onSubmit(this.input)));
        }
    }

    protected void onInputChange(String text) {
        // Nothing by default
    }

    protected void onSubmit(String text) {
        if (this.host != null) this.host.popView();
    }

    // This is a special case called by InventoryHost if this is the active view.
    final void handleAnvilInput(String anvilInput) {
        anvilInput = FontUtil.stripInvalidChars(anvilInput).trim();
        if (anvilInput.equals(this.input)) return;

        this.input = anvilInput;
        onInputChange(anvilInput);
    }
}
