package net.hollowcube.mapmaker.map.action.gui;

import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;

public class ActionEditorView extends Panel {
    private static final int MAX_ACTIONS = 7 * 3;

    private final ActionList actionList = new ActionList();

    public ActionEditorView() {
        super(9, 10);

        background("action/list/container", -10, -31);
        add(0, 0, title("Checkpoint Actions"));

        add(0, 0, backOrClose());
        add(1, 0, info("action.picker"));
        add(2, 0, new Text("todo", 5, 1, "todo")
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/5_1"));
        add(7, 0, new Button("todo", 2, 1)
                .background("generic2/btn/default/2_1"));

        add(1, 2, new ActionListPanel());
    }

    private class ActionListPanel extends Panel {

        public ActionListPanel() {
            super(7, 3);
        }

        @Override
        protected void mount(@NotNull InventoryHost host, boolean isInitial) {
            super.mount(host, isInitial);

            updateContents();
        }

        private void updateContents() {
            // ActionList.ActionData could keep track of whether it was changed and we could be fancier here
            // it probably doesn't matter now.

            // Remove old children (they may have changed)
            clear();

            // Re-add children
            int i = 0;
            for (; i < MAX_ACTIONS; i++) {
                int x = i % 7, y = i / 7;

                var actionData = actionList.get(i);
                if (actionData == null) break;

                //noinspection unchecked
                var translation = ((AbstractAction<Object>) actionData.action()).thumbnail(actionData.getData());
                //noinspection unchecked
                add(x, y, new Button(null, 1, 1)
                        .sprite(((AbstractAction<Object>) actionData.action()).sprite(actionData.getData()))
                        .translationKey(translation.key(), translation.arguments())
                        .lorePostfix(AbstractActionEditorPanel.LORE_POSTFIX_CLICKEDITORREMOVE)
                        .onLeftClick(() -> editExistingAction(actionData))
                        .onRightClick(() -> removeExistingAction(y * 7 + x)));
            }
            if (i < MAX_ACTIONS) {
                int x = i % 7, y = i / 7;
                add(x, y, new Button("gui.action.add", 1, 1)
                        .sprite("generic2/icon/add", 3, 3)
                        .onLeftClick(() -> host.pushTransientView(new ActionPickerView(actionList))));
            }
        }

        private void editExistingAction(@NotNull ActionList.ActionData<?> actionData) {
            //noinspection unchecked Generic Hell :)
            var editor = ((AbstractAction<Object>) actionData.action())
                    .createEditor((ActionList.ActionData<Object>) actionData);
            if (editor.isTransient) host.pushTransientView(editor);
            else host.pushView(editor);
        }

        private void removeExistingAction(int index) {
            actionList.remove(index);
            updateContents();
        }
    }
}
