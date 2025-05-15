package net.hollowcube.mapmaker.panels;

import net.minestom.server.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class AbstractAnvilView extends Panel {

    /**
     * Simple anvil view with an input and submit. The callback is called _after_ popping the view.
     */
    public static @NotNull Panel simpleAnvil(@NotNull String container, @NotNull String icon, @NotNull Consumer<String> onSubmit) {
        return new AbstractAnvilView(container, icon) {
            @Override
            protected void onSubmit(@NotNull String text) {
                super.onSubmit(text);
                onSubmit.accept(text);
            }
        };
    }

    private String input = ""; // todo support initial input (not used for search maps)

    public AbstractAnvilView(@NotNull String container, @NotNull String icon) {
        super(InventoryType.ANVIL, 9, 5);
        background(container, -66, -40);
        add(0, 0, new Button("", 0, 0)
                .background(icon, -46, -1)); // kinda gross

        add(0, 0, new Button("gui.generic.empty", 1, 1)
                .sprite("generic2/anvil/back", -33, 29)
                .onLeftClick(() -> host.popView()));
        add(2, 0, new Button("gui.generic.empty", 1, 1)
                .sprite("generic2/anvil/checkmark", 34, 28)
                .onLeftClick(() -> onSubmit(this.input)));

    }

    protected void onInputChange(@NotNull String text) {
        // Nothing by default
    }

    protected void onSubmit(@NotNull String text) {
        this.host.popView();
    }

    // This is a special case called by InventoryHost if this is the active view.
    final void handleAnvilInput(@NotNull String anvilInput) {
        this.input = anvilInput;
        onInputChange(anvilInput);
    }
}
