package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.canvas.section.RouterSection;
import net.hollowcube.canvas.section.std.AnvilSection;
import net.hollowcube.canvas.section.std.ButtonSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

//todo this is very basic and wont be valid forever, eg for font stuff, checking if a name is valid, etc.
public class GenericNameInput extends AnvilSection {

    private final Consumer<String> callback;

    /**
     * Creates a name input with the given callback.
     * <p>
     * The callback is executed after the player clicks the confirm button, <i>after</i> the history has been popped
     * (eg the current view will be whatever the one was before the anvil).
     */
    public GenericNameInput(@NotNull String initial, @NotNull Consumer<String> callback) {
        this.callback = callback;

        var inputItem = ItemStack.of(Material.PAPER)
                .withDisplayName(Component.text(initial).decoration(TextDecoration.ITALIC, false));

        add(0, 0, new ButtonSection(1, 1, inputItem, () -> {}));
        add(2, 0, new ButtonSection(1, 1, ItemStack.AIR, this::handleConfirm));
    }

    private void handleConfirm() {

        // Return to last view
        var router = find(RouterSection.class);
        router.pop();

        callback.accept(getInput());
    }

}
