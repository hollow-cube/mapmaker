package net.hollowcube.mapmaker.gui.common;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.panels.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ExtraPanels {
    public static final List<Component> LORE_POSTFIX_CLICKSELECT = LanguageProviderV2.translateMulti("gui.action.clickselect", List.of());
    public static final List<Component> LORE_POSTFIX_CLICKEDIT = LanguageProviderV2.translateMulti("gui.action.clickedit", List.of());
    public static final List<Component> LORE_POSTFIX_CLICKCHOOSE = LanguageProviderV2.translateMulti("gui.action.clickchoose", List.of());
    public static final List<Component> LORE_POSTFIX_CLICKCYCLE = LanguageProviderV2.translateMulti("gui.action.clickcycle", List.of());
    public static final List<Component> LORE_POSTFIX_CLICKEDITORREMOVE = LanguageProviderV2.translateMulti("gui.action.clickeditorremove", List.of());
    public static final List<Component> LORE_POSTFIX_CLICKREMOVE = LanguageProviderV2.translateMulti("gui.action.clickremove", List.of());
    public static final List<Component> LORE_POSTFIX_CLICKCREATE = LanguageProviderV2.translateMulti("gui.action.clickcreate", List.of());
    public static final List<Component> LORE_POSTFIX_NOT_AVAILABLE = LanguageProviderV2.translateMulti("gui.action.unavailable", List.of());
    public static final List<Component> LORE_POSTFIX_CLICKCHANGEORREMOVE = LanguageProviderV2.translateMulti("gui.action.clickchangeorremove", List.of());

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

    public static Text infoText(int width, String text) {
        return infoText(width, text, 1);
    }

    public static Text infoText(int width, String text, int xOffset) {
        return new Text(null, width, 1, text)
            .font("small").align(xOffset, 6);
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
