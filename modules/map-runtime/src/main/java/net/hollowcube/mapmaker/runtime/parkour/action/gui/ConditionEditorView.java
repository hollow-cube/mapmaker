package net.hollowcube.mapmaker.runtime.parkour.action.gui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.gui.notifications.NotificationManager;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.MolangExpression;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class ConditionEditorView extends Panel {

    private final ActionTriggerData.Mutable data;

    private final Button messageButton;
    private final ControlledStringInput expressionInput;

    protected ConditionEditorView(ActionTriggerData.Mutable data) {
        super(9, 10);

        this.data = data;

        background("action/list/condition", -10, -31);
        add(0, 0, title("Requirements"));

        add(0, 0, backOrClose());
        add(1, 0, info("condition"));
        add(2, 0, new Text(null, 5, 1, "")
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/5_1")
        );
        this.messageButton = add(7, 0, new Button("gui.action.condition.message", 2, 1)
                .background("generic2/btn/default/2_1")
                .sprite("action/icon/chat", 12, 3)
                .onLeftClick(() -> this.host.pushView(simpleAnvil(
                        "generic2/anvil/field_container",
                        "action/anvil/chat_icon",
                        LanguageProviderV2.translateToPlain("gui.action.condition.name"),
                        message -> {
                            data.setConditionMessage(message);
                            this.onChange();
                        },
                        data.condition().message()
                )))
                .onRightClick(() -> {
                    data.setShowConditionMessage(!data.condition().showMessage());
                    this.onChange();
                })
                .onShiftLeftClick(() -> {
                    data.setConditionMessage("");
                    this.onChange();
                })
        );

        this.expressionInput = add(1, 1, new ControlledStringInput("condition.expression", expr -> {
            data.setConditionExpression(expr.isBlank() ? null : expr);
            var expression = data.condition().expression();
            if (expression != null && expression.error() != null) {
                NotificationManager.showNotification(
                        this.host.player(),
                        "Molang Expression Error",
                        "Invalid expression, please check your syntax.",
                        NamedTextColor.RED
                );
            }
            this.onChange();
        }));
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        this.onChange();
    }

    protected void onChange() {
        var condition = this.data.condition();
        this.messageButton.translationKey(
                "gui.action.condition.message",
                condition.showMessage() ? Component.translatable("gui.action.condition.message.yes") : Component.translatable("gui.action.condition.message.no"),
                condition.message().isBlank() ? Component.translatable("gui.action.condition.message.empty") : Component.text(condition.message())
        );
        this.expressionInput.update(OpUtils.mapOr(condition.expression(), MolangExpression::text, ""));
    }
}
