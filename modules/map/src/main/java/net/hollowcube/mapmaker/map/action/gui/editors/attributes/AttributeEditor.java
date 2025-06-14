package net.hollowcube.mapmaker.map.action.gui.editors.attributes;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.gui.ControlledDecimalInput;
import net.hollowcube.mapmaker.map.action.gui.ControlledTriStateInput;
import net.hollowcube.mapmaker.map.action.impl.EditAttributeAction;
import net.hollowcube.mapmaker.map.action.impl.attributes.ActionAttributes;
import net.hollowcube.mapmaker.map.action.util.Operation;
import org.jetbrains.annotations.NotNull;

public class AttributeEditor extends AbstractActionEditorPanel<EditAttributeAction> {

    private final ControlledTriStateInput<Operation> operation;
    private final ControlledDecimalInput value;

    public AttributeEditor(@NotNull ActionList.Ref ref, ActionAttributes.Entry entry) {
        super(ref);

        this.subtitleText.text(LanguageProviderV2.translateToPlain("gui.action.attribute.%s.name".formatted(entry.attribute().key().value())));

        this.operation = add(1, 1, new ControlledTriStateInput<>("attribute", Operation.class, update(EditAttributeAction::withOperation))
                .label("operation").labels("set", "add", "subtract")
                .sprites(entry.setSprite().withOffset(1, 3),
                        entry.addSprite().withOffset(1, 3),
                        entry.subSprite().withOffset(1, 3)));
        this.operation.update(Operation.SET);
        this.operation.iconButton().onLeftClick(() -> this.host.replaceView(new AttributesEditor(ref)));

        this.value = add(1, 3, new ControlledDecimalInput("attribute", update(EditAttributeAction::withValue))
                .label("value")
                .range(entry.attribute().minValue(), entry.attribute().maxValue())
        );
    }

    @Override
    protected void update(@NotNull EditAttributeAction data) {
        if (data.attribute() == null) return;

        this.operation.update(data.operation());
        this.value.update(data.value());
    }
}
