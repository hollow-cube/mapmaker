package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.std.AnvilSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

//todo this is very basic and wont be valid forever, eg for font stuff, checking if a name is valid, etc.
public class GenericNameInput extends AnvilSection {
    private static final ItemStack INPUT_SLOT = ItemStack.of(Material.PAPER)
            .withDisplayName(Component.text("Untitled").decoration(TextDecoration.ITALIC, false));

    private final Consumer<String> callback;

    /**
     * Creates a name input with the given callback.
     * <p>
     * The callback is executed after the player clicks the confirm button, <i>before</i> the history has been popped
     * (eg the current view will still be the name input).
     */
    public GenericNameInput(@NotNull Consumer<String> callback) {
        this.callback = callback;

        add(0, 0, new ButtonSection(1, 1, INPUT_SLOT, () -> {}));
        add(2, 0, new ButtonSection(1, 1, ItemStack.AIR, this::handleConfirm));
    }

    private void handleConfirm() {
        callback.accept(getInput());

        // Return to last view
        var router = find(RouterSection.class);
        router.pop();
    }

}
