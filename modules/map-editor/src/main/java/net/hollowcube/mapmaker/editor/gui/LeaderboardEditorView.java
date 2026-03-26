package net.hollowcube.mapmaker.editor.gui;

import net.hollowcube.mapmaker.gui.notifications.ToastManager;
import net.hollowcube.mapmaker.map.Leaderboard;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.panels.ui.StringInput;
import net.hollowcube.mapmaker.runtime.parkour.action.MolangExpression;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;
import static net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel.groupText;

public class LeaderboardEditorView extends Panel {
    private final Switch formatSwitch;
    private final Switch orderSwitch;
    private final StringInput expressionInput;

    private final MapData map;
    private Leaderboard leaderboard;
    private MolangExpression expression;

    private boolean editingExpression = false;

    public LeaderboardEditorView(MapData map) {
        super(9, 10);

        background("action/editor/container", -10, -31);
        add(0, 0, title("Edit Leaderboard"));

        add(0, 0, backOrClose());
        add(1, 0, infoWithKey("gui.spawn.customized_leaderboard.information"));
        add(2, 0, new Text(null, 5, 1, "todo") //seriously, what should this say lol
            .align(Text.CENTER, Text.CENTER)
            .background("generic2/btn/default/5_1"));
        add(7, 0, new Button("gui.spawn.customized_leaderboard.action.reset", 2, 1)
            .background("generic2/btn/default/2_1")
            .sprite("icon2/1_1/refresh", 10, 1)
            .onLeftClick(this::onResetToDefault));

        add(1, 1, groupText(4, "format"));
        this.formatSwitch = add(1, 2, new Switch(4, 1, List.of(
            new Text("gui.spawn.customized_leaderboard.format.playtime", 4, 1, "Time")
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/4_1")
                .onLeftClick(() -> onFormatChange(Leaderboard.Format.NUMBER)),
            new Text("gui.spawn.customized_leaderboard.format.number", 4, 1, "Number")
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/4_1")
                .onLeftClick(() -> onFormatChange(Leaderboard.Format.PERCENT)),
            new Text("gui.spawn.customized_leaderboard.format.percent", 4, 1, "Percent")
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/4_1")
                .onLeftClick(() -> onFormatChange(Leaderboard.Format.TIME))
        )));

        add(6, 1, groupText(2, "order"));
        this.orderSwitch = add(6, 2, new Switch(2, 1, List.of(
            new Button("gui.spawn.customized_leaderboard.order.asc", 2, 1)
                .sprite("icon2/1_1/list_up", 10, 1)
                .background("generic2/btn/default/2_1")
                .onLeftClick(() -> onOrderChange(false)),
            new Button("gui.spawn.customized_leaderboard.order.desc", 2, 1)
                .sprite("icon2/1_1/list_down", 10, 1)
                .background("generic2/btn/default/2_1")
                .onLeftClick(() -> onOrderChange(true))
        )));

        add(1, 3, groupText(7, "score expression"));
        this.expressionInput = add(1, 4, new StringInput("gui.spawn.customized_leaderboard.score.expression",
            this::onExpressionChange, () -> editingExpression = true)
            .anvilIcon("action/anvil/variable_expression_icon"));

        this.map = map;
        this.leaderboard = map.settings().leaderboard();
        this.expression = MolangExpression.from(this.leaderboard.score());
        update();
    }

    private void update() {
        this.formatSwitch.select(leaderboard.format().ordinal());
        this.orderSwitch.select(leaderboard.asc() ? 0 : 1);
        this.expressionInput.update(leaderboard.score());
    }

    private void onResetToDefault() {
        if (Leaderboard.DEFAULT.equals(leaderboard)) return;

        leaderboard = Leaderboard.DEFAULT;
        expression = MolangExpression.from(leaderboard.score());
        update();
    }

    private void onFormatChange(Leaderboard.Format format) {
        leaderboard = leaderboard.withFormat(format);
        update();
    }

    private void onOrderChange(boolean asc) {
        leaderboard = leaderboard.withAsc(asc);
        update();
    }

    private void onExpressionChange(String newExpression) {
        leaderboard = leaderboard.withScore(newExpression);
        expression = MolangExpression.from(newExpression);
        update();

        if (expression.error() != null) {
            ToastManager.showNotification(
                this.host.player(),
                Component.text("Molang Expression Error"),
                Component.text("Invalid expression, please check your syntax.", NamedTextColor.RED)
            );
        }
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        editingExpression = false;
    }

    @Override
    protected void unmount() {
        super.unmount();

        // Never save if the expression is invalid
        if (editingExpression || expression.error() != null) return;

        map.settings().setLeaderboard(leaderboard);
    }
}
