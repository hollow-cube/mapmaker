package net.hollowcube.mapmaker.map.action.gui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.ActionRegistry;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.kyori.adventure.key.Key;
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
    }

    @Override
    protected void mount(@NotNull InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        if (!isInitial) return;

        int i = 0;
        for (var actionKey : ActionRegistry.keys()) {
            int x = i % 7, y = i / 7;

            var editor = ActionRegistry.getEditor(actionKey);
            if (editor.exclusiveSet().stream().anyMatch(actionList::has))
                continue; // skip if any exclusive action is already present
            add(x + 1, y + 2, new Button(null, 1, 1)
                    .text(Component.translatable("gui.action." + actionKey.value() + ".title"),
                            LanguageProviderV2.translateMulti("gui.action." + actionKey.value() + ".info.lore", List.of()))
                    .lorePostfix(AbstractActionEditorPanel.LORE_POSTFIX_CLICKSELECT)
                    .sprite(editor.sprite().apply(null))
                    .onLeftClick(() -> this.handleAddAction(actionKey, editor)));
            i++;
        }
    }

    private void handleAddAction(@NotNull Key actionKey, @NotNull Action.Editor<?> editor) {
        var ref = this.actionList.addAction(actionKey);
        var editorFunc = editor.editor();
        if (editorFunc != null) {
            host.pushView(editorFunc.apply(ref));
        } else host.popView();
    }
}
