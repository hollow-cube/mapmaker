package net.hollowcube.mapmaker.gui.common;

import net.hollowcube.mapmaker.panels.*;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public final class ExtraPanels {

    public static Text title(@NotNull String text) {
        return new Text("", 9, 0, text).align(Text.CENTER, -23);
    }

    public static Element backOrClose() {
        return new BackOrClosePanel();
    }

    public static Button info(@NotNull String name) {
        return infoWithKey("gui." + name + ".information");
    }

    public static Button infoWithKey(@NotNull String translationKey) {
        return new Button(translationKey, 1, 1)
            .background("generic2/btn/default/1_1")
            .sprite("generic2/btn/common/info", 4, 2);
    }

    public static Panel confirm(@NotNull Runnable onConfirm) {
        return confirm(null, onConfirm);
    }

    public static Panel confirm(@Nullable String text, @NotNull Runnable onConfirm) {
        return new ConfirmPanel(text, _ -> onConfirm.run());
    }

    public static Panel confirm(@Nullable String text, @NotNull Consumer<Player> onConfirm) {
        return new ConfirmPanel(text, onConfirm);
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

    private static class ConfirmPanel extends Panel {
        public ConfirmPanel(@Nullable String text, @NotNull Consumer<Player> onConfirm) {
            super(3, 1);

            background("generic2/confirm", -10, -13);

            add(4, 2,
                new Text(null, 1, 0, Objects.requireNonNullElse(text, ""))
                    .align(Text.CENTER, 4)
            );

            add(1, 3,
                new Button("gui.confirm2.no", 3, 1)
                    .onLeftClick(() -> host.popOrClose())
            );

            add(5, 3,
                new Button("gui.confirm2.yes", 3, 1)
                    .onLeftClick(() -> {
                        final Player player = host.player();
                        host.popOrClose();
                        onConfirm.accept(player);
                    })
            );
        }
    }

}
