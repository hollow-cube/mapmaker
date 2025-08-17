package net.hollowcube.mapmaker.runtime.parkour.action.gui;

import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionRegistry;
import net.minestom.server.coordinate.Point;
import net.minestom.server.tag.Tag;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;

public class ActionEditorView extends Panel {
    public static final Tag<Point> ACTION_LOCATION = Tag.Transient("action_location");

    private static final int MAX_ACTIONS = 7 * 3;

    private final ActionList actionList;

    public ActionEditorView(ActionList actions, String title) {
        super(9, 10);
        this.actionList = actions;

        background("action/list/container", -10, -31);
        add(0, 0, title(title + " Actions"));

        add(0, 0, backOrClose());
        add(1, 0, info("action"));
        add(2, 0, new Text(null, 5, 1, "Current Actions")
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/5_1"));
        add(7, 0, new Button("gui.action.custom_blocks", 2, 1)
                .background("generic2/btn/default/2_1")
                .sprite("action/icon/cmd", 12, 3));

        add(1, 2, new ActionListPanel());
    }

    private class ActionListPanel extends Panel {

        public ActionListPanel() {
            super(7, 3);
        }

        @Override
        protected void mount(InventoryHost host, boolean isInitial) {
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

                var ref = actionList.get(i);
                if (ref == null) break;

                var editor = (Action.Editor<Action>) ActionRegistry.getEditor(ref.key());

                var translation = editor.thumbnail().apply(ref.action());
                add(x, y, new Button(null, 1, 1)
                        .sprite(editor.sprite().apply(ref.action()))
                        .translationKey(translation.key(), translation.arguments())
                        .lorePostfix(AbstractActionEditorPanel.LORE_POSTFIX_CLICKEDITORREMOVE)
                        .onLeftClick(() -> editExistingAction(ref))
                        .onRightClick(() -> removeExistingAction(y * 7 + x)));
            }
            if (i < MAX_ACTIONS) {
                int x = i % 7, y = i / 7;
                add(x, y, new Button("gui.action.add", 1, 1)
                        .sprite("generic2/icon/add", 3, 3)
                        .onLeftClick(() -> host.pushTransientView(new ActionPickerView(actionList))));
            }
        }

        private void editExistingAction(ActionList.Ref ref) {
            var editorFunc = ActionRegistry.getEditor(ref.key()).editor();
            if (editorFunc != null) {
                host.pushView(editorFunc.apply(ref));
            }
        }

        private void removeExistingAction(int index) {
            actionList.remove(index);
            updateContents();
        }
    }
}
