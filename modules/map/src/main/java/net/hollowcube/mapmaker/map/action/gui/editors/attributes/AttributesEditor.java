package net.hollowcube.mapmaker.map.action.gui.editors.attributes;

import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.impl.EditAttributeAction;
import net.hollowcube.mapmaker.map.action.impl.attributes.ActionAttributes;
import net.hollowcube.mapmaker.panels.Button;
import org.jetbrains.annotations.NotNull;

public class AttributesEditor extends AbstractActionEditorPanel<EditAttributeAction> {

    public AttributesEditor(@NotNull ActionList.Ref ref) {
        super(ref);

        background("action/editor/list_container", -10, -31);

        add(1, 1, AbstractActionEditorPanel.groupText(7, "choose an attribute"));

        int i = 0;
        for (var entry : ActionAttributes.ENTRIES.values()) {
            int x = i % 7, y = i / 7;

            add(x + 1, y + 2, new Button("gui.action.attribute." + entry.id(), 1, 1)
                    .onLeftClick(() -> {
                        this.ref.<EditAttributeAction>update(data -> data.withAttribute(entry.attribute()));
                        this.host.replaceView(new AttributeEditor(this.ref, entry));
                    }))
                    .background(entry.sprite());
            i++;
        }
    }

    @Override
    protected void update(@NotNull EditAttributeAction data) {

    }
}
