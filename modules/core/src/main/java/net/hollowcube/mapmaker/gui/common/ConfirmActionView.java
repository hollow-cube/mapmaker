package net.hollowcube.mapmaker.gui.common;

import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.inventory.InventoryType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class ConfirmActionView extends Panel {

    public ConfirmActionView(Runnable confirmationCallback, TranslatableComponent confirmationText) {
        super(InventoryType.CHEST_3_ROW, 9, 3);

        background("generic/confirm_container");

        add(1, 1, new Button("gui.confirm.no", 3, 1))
            .onLeftClick(() -> this.host.popView());
        add(5, 1, new Button(3, 1)
            .translationKey("gui.confirm.yes", confirmationText)
            .onLeftClick(confirmationCallback::run));
    }
}
