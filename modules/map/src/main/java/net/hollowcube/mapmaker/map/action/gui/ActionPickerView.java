package net.hollowcube.mapmaker.map.action.gui;

import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;

public class ActionPickerView extends Panel {
    private final ActionList actionList;

    public ActionPickerView(@NotNull ActionList actionList) {
        super(9, 10);
        this.actionList = actionList;

        background("action/list/container", -10, -31);
        add(0, 0, title("Choose Action"));

        add(0, 0, backOrClose());
        add(1, 0, info("action.picker"));
        add(2, 0, new Text("todo", 5, 1, "todo")
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/5_1"));
        add(7, 0, new Button("todo", 2, 1)
                .background("generic2/btn/default/2_1"));

        int i = 0;
        for (var action : ActionList.ACTIONS) {
            int x = i % 7, y = i / 7;
            i++;

            add(x + 1, y + 2, new Button(action.key().value(), 1, 1)
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
