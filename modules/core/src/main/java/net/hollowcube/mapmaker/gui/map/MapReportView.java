package net.hollowcube.mapmaker.gui.map;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapReportRequest;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.ReportCategory;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.MultiSelect;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class MapReportView extends Panel {

    private final MapService mapService;
    private final MapData map;

    private final Button commentButton;
    private final MultiSelect<ReportCategory> options;
    private final Button submitButton;

    private String comment = "";

    public MapReportView(MapService mapService, MapData map) {
        super(9, 10);
        this.mapService = mapService;
        this.map = map;

        background("report_map/container", -10, -31);
        add(0, 0, title("Report Map"));

        add(0, 0, backOrClose());
        add(1, 0, info("report_map"));
        var publishedId = MapData.formatPublishedId(map.publishedId());
        add(2, 0, new Text(null, 5, 1, publishedId)
                .align(Text.CENTER, Text.CENTER)
                .sprite("generic2/btn/default/5_1")
                .translationKey("gui.report_map.map_id", publishedId));
        this.commentButton = add(7, 0, new Button(null, 2, 1)
                .background("generic2/btn/default/2_1")
                .sprite("icon2/1_1/speech_bubble", 10, 1)
                .translationKey("gui.report_map.add_comment", getCommentText())
                .onLeftClick(this::handleEditComment));

        this.options = add(1, 2, new MultiSelect<ReportCategory>(6).onChange(this::updateSubmitButton));
        options.addOption(ReportCategory.CHEATED, "gui.report_map.category.cheated", "icon2/1_1/herobrine_face", 1, 1);
        options.addOption(ReportCategory.DISCRIMINATION, "gui.report_map.category.discrimination", "icon2/1_1/angry_face", 1, 1);
        options.addOption(ReportCategory.EXPLICIT_CONTENT, "gui.report_map.category.explicit_content", "icon2/1_1/denied", 1, 1);
        options.addOption(ReportCategory.SPAM, "gui.report_map.category.spam", "icon2/1_1/trash_can", 1, 1);
        options.addOption(ReportCategory.DCMA, "gui.report_map.category.dcma", "icon2/1_1/robber_running", 1, 1);
        options.addOption(ReportCategory.UNPLAYABLE, "gui.report_map.category.unplayable", "icon2/1_1/broken_file", 1, 1);

        this.submitButton = add(2, 4, new Text("gui.report_map.submit.missing_categories", 5, 1, "Submit Report")
                .align(Text.CENTER, Text.CENTER).background("generic2/btn/danger/5_1"))
                .onLeftClickAsync(this::handleSubmit);
    }

    private void handleEditComment() {
        host.pushView(simpleAnvil(
            "generic2/anvil/field_container",
            "report_map/comment_anvil_icon",
            "Report Comment",
            this::handleCommentChange
        ));
    }

    private void handleCommentChange(String newComment) {
        newComment = newComment.trim();
        if (this.comment.equals(newComment)) return;

        this.comment = newComment;
        this.commentButton.translationKey("gui.report_map.add_comment", getCommentText());
        if (canSubmit()) updateSubmitButton(); // No submit version doesnt use comment text.
    }

    private void updateSubmitButton() {
        if (canSubmit()) {
            var title = Component.translatable("gui.report_map.submit.name");
            var lore = new ArrayList<>(LanguageProviderV2.translateMulti("gui.report_map.submit.header", List.of()));
            for (var category : options.selectedItems())
                lore.add(Component.translatable("gui.report_map.submit.category",
                        Component.translatable("gui.report_map.category." + category.name().toLowerCase() + ".on.name")));
            lore.addAll(LanguageProviderV2.translateMulti("gui.report_map.submit.footer", List.of(getCommentText())));

            this.submitButton.background("generic2/btn/success/5_1");
            this.submitButton.text(title, lore);
        } else {
            this.submitButton.background("generic2/btn/danger/5_1");
            this.submitButton.translationKey("gui.report_map.submit.cannot_submit");
        }
    }

    private void handleSubmit() {
        if (!canSubmit()) return;

        var player = host.player();
        player.closeInventory();
        var playerId = PlayerData.fromPlayer(player).id();
        var req = new MapReportRequest(playerId, new ArrayList<>(options.selectedItems()), comment, null, null);
        try {
            mapService.reportMap(map.id(), req);
            player.sendMessage(Component.translatable("gui.report_map.submit.success"));
        } catch (Exception e) {
            player.sendMessage(Component.translatable("gui.report_map.submit.failure"));
            ExceptionReporter.reportException(e, player);
        }
    }

    private boolean canSubmit() {
        var categories = options.selectedItems();
        if (categories.isEmpty()) return false;
        for (var category : categories) {
            if (category.requiresComment() && comment.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Component getCommentText() {
        return comment.isEmpty()
            ? Component.translatable("gui.report_map.add_comment.none")
            : Component.text(comment);
    }
}
