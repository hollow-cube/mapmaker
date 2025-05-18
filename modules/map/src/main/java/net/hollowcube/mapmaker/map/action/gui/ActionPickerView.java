package net.hollowcube.mapmaker.map.action.gui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;

public class ActionPickerView extends Panel {
    private final ActionList actionList;

    public ActionPickerView(@NotNull ActionList actionList) {
        super(9, 10);
        this.actionList = actionList;

        background("action/list/container", -10, -31);
        add(0, 0, title("Add Action"));
        add(1, 1, AbstractActionEditorPanel.groupText(7, "choose action"));

        add(0, 0, backOrClose());
        add(1, 0, info("action"));
        add(2, 0, new Text("gui.action.search.empty", 7, 1, "Search...")
                .align(8, 5).background("action/editor/search_bar")); // TODO: search support

        int i = 0;
        for (var action : ActionList.ACTIONS) {
            int x = i % 7, y = i / 7;
            i++;

            add(x + 1, y + 2, new Button(action.key().value(), 1, 1)
                    .text(Component.translatable("gui.action." + action.key().value() + ".title"),
                            LanguageProviderV2.translateMulti("gui.action." + action.key().value() + ".info.lore", List.of()))
                    .lorePostfix(AbstractActionEditorPanel.LORE_POSTFIX_CLICKSELECT)
                    .sprite(action.sprite(null))
                    .onLeftClick(() -> this.handleAddAction(action)));
        }
    }

    private void handleAddAction(@NotNull AbstractAction<?> action) {
        var actionData = this.actionList.addAction(action);

        //noinspection unchecked Generic Hell :)
        var editor = ((AbstractAction<Object>) actionData.action())
                .createEditor((ActionList.ActionData<Object>) actionData);
        if (editor.isTransient) host.pushTransientView(editor);
        else host.pushView(editor);
    }
}
