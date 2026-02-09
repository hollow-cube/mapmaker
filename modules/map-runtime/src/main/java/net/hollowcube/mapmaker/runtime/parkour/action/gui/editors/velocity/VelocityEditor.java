package net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.velocity;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.math.relative.RelativeField;
import net.hollowcube.mapmaker.gui.notifications.ToastManager;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ControlledDecimalInput;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.EditVelocityAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.velocity.VelocityModifiers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class VelocityEditor extends AbstractActionEditorPanel<EditVelocityAction> {

    private final RelativeFieldInput yawInput;
    private final RelativeFieldInput pitchInput;
    private final ControlledDecimalInput powerInput;

    public VelocityEditor(ActionList.Ref ref) {
        super(ref);

        background("action/editor/velocity_container", -10, -31);

        this.yawInput = add(1, 1, new RelativeFieldInput("velocity.yaw", updateField(EditVelocityAction::withYaw)));
        this.pitchInput = add(5, 1, new RelativeFieldInput("velocity.pitch", updateField(EditVelocityAction::withPitch)));
        this.powerInput = add(1, 3, new ControlledDecimalInput("velocity.power", update(EditVelocityAction::withPower)));
    }

    public static TranslatableComponent thumbnail(@Nullable EditVelocityAction action) {
        if (action != null && action.modifier() instanceof VelocityModifiers.Molang) {
            return Component.translatable("gui.action.velocity.thumbnail.invalid");
        } else if (action != null && action.modifier() instanceof VelocityModifiers.DirectionPower modifier) {
            var yaw = modifier.yaw();
            var pitch = modifier.pitch();
            var power = modifier.power();
            return Component.translatable(
                "gui.action.velocity.thumbnail",
                Component.text(yaw.toDisplayString()),
                Component.text(pitch.toDisplayString()),
                Component.text(power)
            );
        } else {
            return Component.translatable("gui.action.velocity.thumbnail.empty");
        }
    }

    private Consumer<String> updateField(BiFunction<EditVelocityAction, RelativeField, EditVelocityAction> updateFunc) {
        return update((data, value) -> {
            try {
                return updateFunc.apply(data, RelativeField.fromString(value));
            } catch (NumberFormatException ignored) {
                return data; // Do nothing
            }
        });
    }

    @Override
    protected void update(EditVelocityAction data) {
        if (data.modifier() instanceof VelocityModifiers.DirectionPower(
            RelativeField yaw, RelativeField pitch, double power
        )) {
            this.yawInput.update(yaw.toString());
            this.pitchInput.update(pitch.toString());
            this.powerInput.update(power);
        } else {
            ToastManager.showNotification(
                this.host.player(),
                Component.text("Invalid modifier", NamedTextColor.RED),
                Component.text("Please report this as a bug.", NamedTextColor.GRAY)
            );
        }
    }

    private static class RelativeFieldInput extends Panel {
        private final Consumer<String> onChange;

        private final Text inputText;
        private final String anvilTitle;

        private String value = "";

        public RelativeFieldInput(String key, Consumer<String> onChange) {
            super(3, 2);
            this.onChange = onChange;

            var translationKey = "gui.action." + key;
            var label = LanguageProviderV2.translateToPlain(translationKey + ".label");
            this.anvilTitle = LanguageProviderV2.translateToPlain(translationKey + ".name");

            // TODO fix that the labels actually dont have a small font variant
            add(0, 0, new Text(translationKey, 3, 1, label).font("small").align(1, 6));
            this.inputText = add(0, 1, new Text(translationKey, 3, 1, "").align(6, 5));
            this.inputText.onLeftClick(this::handleEditValue);
        }

        public void update(String value) {
            if ("~0.0".equals(value)) value = "";

            this.value = value;
            this.inputText.text(normalizeForDisplay(value));
        }

        private void handleEditValue() {
            host.pushView(simpleAnvil(
                "generic2/anvil/field_container",
                "action/anvil/coordinates_icon",
                anvilTitle, onChange, value
            ));
        }

        private String normalizeForDisplay(String value) {
            if (value.isEmpty()) return "~";
            String[] parts = value.split("\\.");
            if (parts.length == 1) return value;
            // Drop trailing 0
            if ("0".equals(parts[1])) return parts[0];

            int allowedPostLength = 5 - parts[0].length();
            if (allowedPostLength <= 0) return parts[0];
            if (parts[1].length() > allowedPostLength) {
                return parts[0] + "." + parts[1].substring(0, allowedPostLength);
            }

            return value;
        }

    }
}