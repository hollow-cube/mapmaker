package net.hollowcube.mapmaker.gui.totp;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.gui.common.anvil.TextInputView;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TotpInputViews {

    private static final Component CLICK_TO_COPY = Component.text("Click to copy").color(NamedTextColor.GRAY);
    private static final Component INVALID_FORMAT = Component.text("Invalid format").color(TextColor.color(0xFF0000));
    private static final Component INVALID_CODE = Component.text("Invalid code").color(TextColor.color(0xFF0000));

    public static Book backupCodesBook(@NotNull String[] codes) {
        var component = Component.text();

        component.append(Component.text(BadSprite.require("totp/codes").fontChar()).shadowColor(ShadowColor.none()));

        component.appendNewline().appendNewline().appendNewline().appendNewline().appendNewline();

        for (String code : codes) {
            int offset = 22 + (90 - FontUtil.measureText(code)) / 2;
            component.append(Component.text(FontUtil.computeOffset(offset))
                    .append(Component.text(code, NamedTextColor.WHITE))
                    .hoverEvent(HoverEvent.showText(CLICK_TO_COPY))
                    .clickEvent(ClickEvent.copyToClipboard(String.join("\n", codes))));
            component.appendNewline();
        }

        return Book.builder().addPage(component.build()).build();
    }

    public static Function<Context, View> inputView(
            @NotNull Component title,
            @NotNull BiFunction<String, String, PlayerService.TotpResult> checker,
            @NotNull BiConsumer<TextInputView, PlayerService.TotpResult> callback
    ) {
        return context -> TextInputView.builder()
                .icon("anvil/earth")
                .title(title)
                .callback((view, code) -> {
                    String playerId = context.player().getUuid().toString();
                    String truncatedCode = code.replace(" ", "").replace("-", "");

                    switch (checker.apply(playerId, truncatedCode)) {
                        case SUCCESS -> callback.accept(view, PlayerService.TotpResult.SUCCESS);
                        case INVALID_FORMAT -> view.pushTransientView(inputView(INVALID_FORMAT, checker, callback));
                        case INVALID_CODE -> view.pushTransientView(inputView(INVALID_CODE, checker, callback));
                        case NOT_ENABLED -> callback.accept(view, PlayerService.TotpResult.NOT_ENABLED);
                        case ALREADY_ENABLED -> callback.accept(view, PlayerService.TotpResult.ALREADY_ENABLED);
                    }
                })
                .build(context);
    }
}
