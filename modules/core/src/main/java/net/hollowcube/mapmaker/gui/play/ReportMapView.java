package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.*;
import net.hollowcube.canvas.annotation.*;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapReportRequest;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.ReportCategory;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class ReportMapView extends View {

    private @ContextObject MapService mapService;

    private @Outlet("title") Text titleText;
    private @Outlet("map_id_text") Text mapIdText;
    private @OutletGroup("switch_.+") Switch[] categorySwitches;
    private @OutletGroup("reason_.+_off") Label[] reasonOffButtons;
    private @OutletGroup("reason_.+_on") Label[] reasonOnButtons;
    private @Outlet("add_comment") Label addCommentButton;
    private @Outlet("submit_switch") Switch submitStateSwitch;
    private @Outlet("submit") Label submitButton;

    private final String mapId;
    private final EnumSet<ReportCategory> categories = EnumSet.noneOf(ReportCategory.class);
    private String comment = "";

    //todo handle position and context (context should contain debugging things like current server, etc)

    public ReportMapView(@NotNull Context context, @NotNull MapData map) {
        super(context);
        this.mapId = map.id();

        String mapId = MapData.formatPublishedId(map.publishedId());

        titleText.setText("Report Map");
        mapIdText.setText(mapId);
        mapIdText.setArgs(Component.text(mapId));

        initActions();
        updateCommentText("");
        updateSubmitButton();
    }

    public boolean canSubmit() {
        return !categories.isEmpty();
    }

    private void initActions() {
        for (var reason : ReportCategory.values()) {
            var offButtonId = Objects.requireNonNull(reasonOffButtons[reason.ordinal()].id());
            addActionHandler(offButtonId, Label.ActionHandler.lmb($ -> selectCategory(reason)));

            var onButtonId = Objects.requireNonNull(reasonOnButtons[reason.ordinal()].id());
            addActionHandler(onButtonId, Label.ActionHandler.lmb($ -> deselectCategory(reason)));
        }
    }

    private void deselectCategory(@NotNull ReportCategory category) {
        if (!categories.contains(category)) return;
        categories.remove(category);
        categorySwitches[category.ordinal()].setOption(0);
        updateSubmitButton();
    }

    private void selectCategory(@NotNull ReportCategory category) {
        if (categories.contains(category)) return;
        categories.add(category);
        categorySwitches[category.ordinal()].setOption(1);
        updateSubmitButton();
    }

    private void updateSubmitButton() {
        var title = Component.translatable("gui.report_map.submit.name");
        List<Component> lore = new ArrayList<>();

        if (canSubmit()) {
            submitStateSwitch.setOption(1);
            lore.addAll(LanguageProviderV2.translateMulti("gui.report_map.submit.header", List.of()));
            for (var category : categories) {
                lore.add(Component.translatable("gui.report_map.submit.category",
                        Component.translatable("gui.report_map.category." + category.name().toLowerCase() + ".on.name")));
            }

            lore.addAll(LanguageProviderV2.translateMulti("gui.report_map.submit.footer", List.of(getCommentText())));
        } else {
            submitStateSwitch.setOption(0);
        }

        submitButton.setComponentsDirect(title, lore);
    }

    @Action("add_comment")
    private void handleAddComment(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT_CLICK || clickType == ClickType.RIGHT_CLICK) {
            pushView(c -> new SetReportCommentView(c, comment));
        } else if (clickType == ClickType.SHIFT_LEFT_CLICK) {
            updateCommentText("");
        }
    }

    @Signal(SetReportCommentView.SIG_UPDATE_NAME)
    private void updateCommentText(@NotNull String text) {
        this.comment = text;

        addCommentButton.setArgs(getCommentText());
        updateSubmitButton();
    }

    @Action("submit")
    private void handleSubmit(@NotNull Player player) {
        // Do not submit if there is an inflight request
        if (!canSubmit() || submitButton.getState() == State.LOADING) return;

        submitButton.setState(State.LOADING);
        async(() -> {
            var playerData = PlayerDataV2.fromPlayer(player);
            var req = new MapReportRequest(
                    playerData.id(),
                    new ArrayList<>(categories),
                    comment,
                    null,
                    null
            );
            try {
                mapService.reportMap(mapId, req);
                player.sendMessage(Component.translatable("gui.report_map.submit.success"));
            } catch (Exception e) {
                player.sendMessage(Component.translatable("gui.report_map.submit.failure"));
                MinecraftServer.getExceptionManager().handleException(e);
            } finally {
                submitButton.setState(State.ACTIVE);
            }
        });
    }

    private @NotNull Component getCommentText() {
        return comment.isEmpty()
                ? Component.translatable("gui.report_map.add_comment.none")
                : Component.text(comment);
    }

}
