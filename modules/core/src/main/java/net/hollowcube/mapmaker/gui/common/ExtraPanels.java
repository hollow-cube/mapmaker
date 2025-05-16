package net.hollowcube.mapmaker.gui.common;

import net.hollowcube.mapmaker.panels.*;
import org.jetbrains.annotations.NotNull;

public final class ExtraPanels {

    public static Text title(@NotNull String text) {
        return new Text("", 9, 0, text).align(Text.CENTER, -23);
    }

    public static Element backOrClose() {
        return new BackOrClosePanel();
    }

    public static Button info(@NotNull String name) {
        return new Button("gui." + name + ".information", 1, 1)
                .background("generic2/btn/default/1_1")
                .sprite("generic2/btn/common/info", 4, 2);
    }

    private static class BackOrClosePanel extends Panel {
        private final Button button;

        public BackOrClosePanel() {
            super(1, 1);

            this.button = add(0, 0, new Button(null, 1, 1)
                    .background("generic2/btn/danger/1_1")
                    .onLeftClick(this::handleClick));
        }

        private void handleClick() {
            if (host.canPopView()) {
                host.popView();
            } else {
                host.player().closeInventory();
            }
        }

        @Override
        protected void mount(@NotNull InventoryHost host, boolean isInitial) {
            super.mount(host, isInitial);

            if (host.canPopView()) {
                button.translationKey("gui.generic.back_arrow");
                button.sprite("generic2/btn/common/back", 5, 3);
            } else {
                button.translationKey("gui.generic.close_menu");
                button.sprite("generic2/btn/common/close", 4, 4);
            }
        }
    }

}
