package net.hollowcube.common.dialogs;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogActionButton;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public final class DialogButtons {

    private static final DialogAction.Custom CLOSE_ACTION = new DialogAction.Custom(
            Key.key("hollowcube", "close_dialog"),
            null
    );

    private DialogButtons() {
    }

    public static DialogActionButton button(Component component, int width, Key key, @Nullable CompoundBinaryTag extra) {
       return new DialogActionButton(
               LanguageProviderV2.translate(component),
               null,
                width,
               new DialogAction.DynamicCustom(key, extra)
       );
    }

    public static DialogActionButton close(Component component, int width) {
        return new DialogActionButton(
                LanguageProviderV2.translate(component), null, width,
                DialogButtons.CLOSE_ACTION
        );
    }

    @ApiStatus.Internal
    public static void init(GlobalEventHandler events) {
        events.addListener(PlayerCustomClickEvent.class, event -> {
            if (event.getKey().equals(CLOSE_ACTION.key())) {
                event.getPlayer().closeDialog();
            }
        });
    }
}
