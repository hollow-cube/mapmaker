package net.hollowcube.mapmaker.gui.totp;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.panels.AbstractAnvilView;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;

import java.util.function.BiFunction;
import java.util.function.Consumer;

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

    public static Book backupCodesBook(String[] codes) {
        var component = Component.text();

        component.append(Component.text(BadSprite.require("totp/codes").fontChar()).color(NamedTextColor.WHITE).shadowColor(ShadowColor.none()));

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
}
