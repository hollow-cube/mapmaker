package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.util.RelativePos;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.panels.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public class TeleportAction extends AbstractAction<RelativePos> {
    public static final TeleportAction INSTANCE = new TeleportAction();

    private static final String RELATIVE_ZERO = "~0.0";

    private static final Sprite SPRITE_DEFAULT = new Sprite("action/icon/teleport", 3, 3);

    public TeleportAction() {
        super("mapmaker:teleport", RelativePos.CODEC, RelativePos.REL_ZERO);
    }

    @Override
    public @NotNull Sprite sprite(@Nullable RelativePos ignored) {
        return SPRITE_DEFAULT;
    }

    @Override
    public @NotNull TranslatableComponent thumbnail(@Nullable RelativePos data) {
        if (data == null) return Component.translatable("gui.action.teleport.thumbnail.empty");
        return Component.translatable("gui.action.teleport.thumbnail", List.of(
                tildeOnly(data.strX()), tildeOnly(data.strY()), tildeOnly(data.strZ()),
                tildeOnly(data.strYaw()), tildeOnly(data.strPitch())
        ));
    }

    private static @NotNull Component tildeOnly(@NotNull String value) {
        return Component.text(RELATIVE_ZERO.equals(value) ? "~" : value);
    }

    @Override
    public @NotNull AbstractActionEditorPanel<RelativePos> createEditor(@NotNull ActionList.ActionData<RelativePos> actionData) {
        return new Editor(actionData);
    }

    private static class Editor extends AbstractActionEditorPanel<RelativePos> {
        private final TexturelessNumberInput xInput;
        private final TexturelessNumberInput yInput;
        private final TexturelessNumberInput zInput;
        private final TexturelessNumberInput yawInput;
        private final TexturelessNumberInput pitchInput;

        public Editor(@NotNull ActionList.ActionData<RelativePos> actionData) {
            super(actionData);

            background("action/editor/teleport_container", -10, -31);

            this.xInput = add(1, 1, new TexturelessNumberInput(2, 0, "x", safeUpdate(RelativePos::withStrX)));
            this.yInput = add(3, 1, new TexturelessNumberInput(2, 6, "y", safeUpdate(RelativePos::withStrY)));
            this.zInput = add(5, 1, new TexturelessNumberInput(3, 12, "z", safeUpdate(RelativePos::withStrZ)));

            this.yawInput = add(1, 3, new TexturelessNumberInput(2, 0, "yaw", update(RelativePos::withStrYaw)));
            this.pitchInput = add(3, 3, new TexturelessNumberInput(2, 6, "pitch", update(RelativePos::withStrPitch)));
            add(5, 4, new Button("gui.action.teleport.command", 3, 1));
            // TODO add functionality to command button
        }

        @Override
        protected void update(@NotNull RelativePos data) {
            this.xInput.update(data.strX());
            this.yInput.update(data.strY());
            this.zInput.update(data.strZ());
            this.yawInput.update(data.strYaw());
            this.pitchInput.update(data.strPitch());
        }

        private @NotNull Consumer<String> safeUpdate(@NotNull BiFunction<RelativePos, String, RelativePos> updateFunc) {
            return update((data, value) -> {
                try {
                    return updateFunc.apply(data, value);
                } catch (NumberFormatException ignored) {
                    return data; // Do nothing
                }
            });
        }
    }

    /// The input used for teleport coordinates,
    private static class TexturelessNumberInput extends Panel {
        private final Consumer<String> onChange;

        private final Text inputText;

        private String value = "";

        public TexturelessNumberInput(int width, int xOffset, String label, @NotNull Consumer<String> onChange) {
            super(width, 2);
            this.onChange = onChange;

            var translationKey = "gui.action.teleport." + label;
            add(0, 0, new Text(translationKey, width, 1, LanguageProviderV2.translateToPlain(translationKey))
                    .font("small").align(1 + xOffset, 6));
            this.inputText = add(0, 1, new Text(translationKey, width, 1, "")
                    .align(xOffset + 6, 5));
            this.inputText.onLeftClick(this::handleEditValue);
        }

        public void update(@NotNull String value) {
            if (RELATIVE_ZERO.equals(value)) value = "";
            if (this.value.equals(value)) return;

            this.value = value;
            this.inputText.text(normalizeForDisplay(value));
        }

        private void handleEditValue() {
            host.pushView(simpleAnvil(
                    "generic2/anvil/field_container",
                    "action/anvil/teleport_icon",
                    onChange, value
            ));
        }

        private @NotNull String normalizeForDisplay(@NotNull String value) {
            if (value.isEmpty()) return "";
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
