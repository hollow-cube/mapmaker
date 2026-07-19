package net.hollowcube.mapmaker.gui.totp;

import net.hollowcube.common.dialogs.DialogBuilder;
import net.hollowcube.common.dialogs.DialogButtons;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.panels.AbstractAnvilView;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.dialog.Dialog;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.function.BiFunction;
import java.util.function.Consumer;

@NotNullByDefault
public class TotpInputView extends AbstractAnvilView {

    private static final Component CLICK_TO_COPY = Component.text("Click to copy").color(NamedTextColor.GRAY);

    private final BiFunction<String, String, PlayerService.TotpResult> checker;
    private final Consumer<PlayerService.TotpResult> callback;

    public TotpInputView(String title, BiFunction<String, String, PlayerService.TotpResult> checker, Consumer<PlayerService.TotpResult> callback) {
        super("generic2/anvil/field_container", "map_browser/search_anvil_icon", title, "", true);

        this.checker = checker;
        this.callback = callback;
    }

    @Override
    protected void onSubmit(String code) {
        String playerId = this.host.player().getUuid().toString();
        String truncatedCode = code.replace(" ", "").replace("-", "");

        FutureUtil.submitVirtual(() -> {
            switch (checker.apply(playerId, truncatedCode)) {
                case SUCCESS -> callback.accept(PlayerService.TotpResult.SUCCESS);
                case INVALID_FORMAT ->
                    this.host.pushTransientView(new TotpInputView("Invalid format", checker, callback));
                case INVALID_CODE -> this.host.pushTransientView(new TotpInputView("Invalid code", checker, callback));
                case NOT_ENABLED -> callback.accept(PlayerService.TotpResult.NOT_ENABLED);
                case ALREADY_ENABLED -> callback.accept(PlayerService.TotpResult.ALREADY_ENABLED);
            }
        });
    }

    // The 131px sprite hangs below line 1's baseline (ascent 0), but the dialog only reserves
    // the body's text height: fill enough 9px lines to cover it. Codes overlay the sprite's
    // box starting at line 5; the dialog centers each line so no x offsets are needed.
    private static final int CODES_LINES = 16;
    private static final int CODES_START_LINE = 5;

    public static Dialog backupCodesDialog(String[] codes) {
        var component = Component.text();

        component.append(Component.text(BadSprite.require("totp/codes").fontChar(), NamedTextColor.WHITE)
            .shadowColor(ShadowColor.none()));
        component.append(Component.text("\n".repeat(CODES_START_LINE)));

        var copyAll = ClickEvent.copyToClipboard(String.join("\n", codes));
        for (int i = 0; i < codes.length; i++) {
            if (i > 0) component.append(Component.text("\n"));
            component.append(Component.text(codes[i], NamedTextColor.WHITE)
                .hoverEvent(HoverEvent.showText(CLICK_TO_COPY))
                .clickEvent(copyAll));
        }
        component.append(Component.text("\n".repeat(
            Math.max(0, CODES_LINES - CODES_START_LINE - codes.length))));

        return DialogBuilder.create()
            .title(Component.translatable("dialog.totp.codes.title"))
            .closeOnEscape()
            .body(it -> it.text(component.build(), 150))
            .buildNotice(DialogButtons.close(Component.translatable("dialog.generic.close"), 150));
    }
}
