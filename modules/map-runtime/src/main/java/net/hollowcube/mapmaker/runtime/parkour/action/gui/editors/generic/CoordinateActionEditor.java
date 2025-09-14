package net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.generic;

import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.base.CoordinateAction;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class CoordinateActionEditor<T extends CoordinateAction<T>> extends AbstractActionEditorPanel<@NotNull T> {

    private final TexturelessNumberInput xInput;
    private final TexturelessNumberInput yInput;
    private final TexturelessNumberInput zInput;
    private final TexturelessNumberInput yawInput;
    private final TexturelessNumberInput pitchInput;
    private final Button commandButton;

    public CoordinateActionEditor(ActionList.Ref ref) {
        super(ref);

        background("action/editor/coordinates_container", -10, -31);

        subtitleText.text("Set Coords");

        this.xInput = add(1, 1, new TexturelessNumberInput(2, 0, "x", safeUpdate(RelativePos::withX)));
        this.yInput = add(3, 1, new TexturelessNumberInput(2, 6, "y", safeUpdate(RelativePos::withY)));
        this.zInput = add(5, 1, new TexturelessNumberInput(3, 12, "z", safeUpdate(RelativePos::withZ)));

        this.yawInput = add(1, 3, new TexturelessNumberInput(2, 0, "yaw", safeUpdate(RelativePos::withYaw)));
        this.pitchInput = add(3, 3, new TexturelessNumberInput(2, 6, "pitch", safeUpdate(RelativePos::withPitch)));
        this.commandButton = add(5, 4, new Button("gui.action.coordinates.command", 3, 1)
                .onLeftClick(this::beginCommandUpdate));
    }

    @Override
    protected void update(T action) {
        this.xInput.update(action.target().x());
        this.yInput.update(action.target().y());
        this.zInput.update(action.target().z());
        this.yawInput.update(action.target().yaw());
        this.pitchInput.update(action.target().pitch());

        commandButton.lorePostfix(
                host.hasTag(CoordinateAction.SPC_TARGET_TAG) ? LORE_POSTFIX_CLICKEDIT : LORE_POSTFIX_NOT_AVAILABLE);
    }

    private void beginCommandUpdate() {
        var spcTarget = host.getTag(CoordinateAction.SPC_TARGET_TAG);
        if (spcTarget == null) return;

        host.player().sendMessage(Component.translatable("command.set_precise_coords.begin"));
        host.player().setTag(CoordinateAction.SPC_EDIT_TAG, Pair.of(this.ref.key(), spcTarget));
        host.player().closeInventory();
    }

    private Consumer<String> safeUpdate(BiFunction<RelativePos, String, RelativePos> updateFunc) {
        return update((data, value) -> {
            try {
                return data.withTarget(updateFunc.apply(data.target(), value));
            } catch (NumberFormatException ignored) {
                return data; // Do nothing
            }
        });
    }

    private static class TexturelessNumberInput extends Panel {
        private final Consumer<String> onChange;

        private final Text inputText;
        private final String anvilTitle;

        private String value = "";

        public TexturelessNumberInput(int width, int xOffset, String key, Consumer<String> onChange) {
            super(width, 2);
            this.onChange = onChange;

            var translationKey = "gui.action.coordinates." + key;
            var label = LanguageProviderV2.translateToPlain(translationKey + ".label");
            this.anvilTitle = LanguageProviderV2.translateToPlain(translationKey + ".name");

            // TODO fix that the labels actually dont have a small font variant
            add(0, 0, new Text(translationKey, width, 1, label).font("small").align(1 + xOffset, 6));
            this.inputText = add(0, 1, new Text(translationKey, width, 1, "")
                    .align(xOffset + 6, 5));
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