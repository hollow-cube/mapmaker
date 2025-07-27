package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.attributes.AttributeEditor;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.attributes.AttributesEditor;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.attributes.ActionAttributes;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.attributes.AttributeMap;
import net.hollowcube.mapmaker.runtime.parkour.action.util.Operation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslationArgument;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public record EditAttributeAction(
        @Nullable Attribute attribute,
        Operation operation,
        double value
) implements Action {

    public static final Key KEY = Key.key("mapmaker:attribute");
    public static final StructCodec<EditAttributeAction> CODEC = StructCodec.struct(
            "attribute", Attribute.CODEC.optional(), EditAttributeAction::attribute,
            "operation", Operation.CODEC.optional(Operation.ADD), EditAttributeAction::operation,
            "value", StructCodec.DOUBLE.optional(0.0), EditAttributeAction::value,
            EditAttributeAction::new
    );

    public static final PlayState.Attachment<AttributeMap> SAVE_DATA = PlayState.attachment(KEY, AttributeMap.CODEC);
    public static final Editor<EditAttributeAction> EDITOR = new Editor<>(
            it -> {
                var attribute = it.<EditAttributeAction>cast().attribute();
                return attribute == null ? new AttributesEditor(it) : new AttributeEditor(it, ActionAttributes.ENTRIES.get(attribute));
            },
            new Sprite("action/icon/attribute", 2, 3),
            action -> {
                if (action == null || action.attribute() == null)
                    return Component.translatable("gui.action.attribute.thumbnail.empty");
                return Component.translatable("gui.action.attribute.thumbnail", List.of(
                        Component.translatable("gui.action.attribute.%s.name".formatted(action.attribute().key().value())),
                        Component.translatable("gui.action.attribute.%s.label".formatted(action.operation().name().toLowerCase(Locale.ROOT))),
                        TranslationArgument.numeric(action.value)
                ));
            }
    );

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        if (this.attribute == null) return;

        var current = Objects.requireNonNullElseGet(state.get(SAVE_DATA), AttributeMap::new);
        if (this.operation == Operation.SET) {
            current = current.with(this.attribute, this.value);
        } else {
            var value = current.getOrDefault(this.attribute, this.attribute.defaultValue());
            current = current.with(this.attribute, this.operation.apply(value, this.value));
        }

        state.set(SAVE_DATA, current);
    }

    public EditAttributeAction withAttribute(Attribute attribute) {
        return new EditAttributeAction(attribute, this.operation, this.value);
    }

    public EditAttributeAction withOperation(Operation operation) {
        return new EditAttributeAction(this.attribute, operation, this.value);
    }

    public EditAttributeAction withValue(double value) {
        return new EditAttributeAction(this.attribute, this.operation, value);
    }
}
