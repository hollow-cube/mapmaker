package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public record TeleportAction(RelativePos target) implements Action {
    private static final Sprite SPRITE_DEFAULT = new Sprite("action/icon/teleport", 3, 3);
    private static final String RELATIVE_ZERO = "~0.0";
    private static final Sound TELEPORT_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_TELEPORT, Sound.Source.PLAYER, 0.5f, 1f);

    public static final Tag<Object> SPC_TAG = Tag.Transient("mapmaker:spc/tag");

    public static final Key KEY = Key.key("mapmaker:teleport");
    public static final StructCodec<TeleportAction> CODEC = StructCodec.struct(
            StructCodec.INLINE, RelativePos.STRUCT_CODEC.optional(RelativePos.ORIGIN), TeleportAction::target,
            TeleportAction::new);
    public static final Action.Editor<TeleportAction> EDITOR = new Action.Editor<>(
            TeleportAction.Editor::new, _ -> SPRITE_DEFAULT,
            TeleportAction::makeThumbnail, Set.of(KEY));

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        player.teleport(target.pos(), Vec.ZERO, null, target.flags());
        player.playSound(TELEPORT_SOUND, target.resolve(player.getPosition()));
    }

    private static TranslatableComponent makeThumbnail(@Nullable TeleportAction action) {
        if (action == null) return Component.translatable("gui.action.teleport.thumbnail.empty");
        return Component.translatable("gui.action.teleport.thumbnail", List.of(
                tildeOnly(action.target.x()), tildeOnly(action.target.y()), tildeOnly(action.target.z()),
                tildeOnly(action.target.yaw()), tildeOnly(action.target.pitch())
        ));
    }

    private static Component tildeOnly(String value) {
        return Component.text(RELATIVE_ZERO.equals(value) ? "~" : value);
    }

    private static class Editor extends AbstractActionEditorPanel<TeleportAction> {
        private final TexturelessNumberInput xInput;
        private final TexturelessNumberInput yInput;
        private final TexturelessNumberInput zInput;
        private final TexturelessNumberInput yawInput;
        private final TexturelessNumberInput pitchInput;
        private final Button commandButton;

        public Editor(ActionList.Ref ref) {
            super(ref);

            background("action/editor/teleport_container", -10, -31);

            subtitleText.text("Set Coords");

            this.xInput = add(1, 1, new TexturelessNumberInput(2, 0, "x", safeUpdate(RelativePos::withX)));
            this.yInput = add(3, 1, new TexturelessNumberInput(2, 6, "y", safeUpdate(RelativePos::withY)));
            this.zInput = add(5, 1, new TexturelessNumberInput(3, 12, "z", safeUpdate(RelativePos::withZ)));

            this.yawInput = add(1, 3, new TexturelessNumberInput(2, 0, "yaw", safeUpdate(RelativePos::withYaw)));
            this.pitchInput = add(3, 3, new TexturelessNumberInput(2, 6, "pitch", safeUpdate(RelativePos::withPitch)));
            this.commandButton = add(5, 4, new Button("gui.action.teleport.command", 3, 1)
                    .onLeftClick(this::beginCommandUpdate));
        }

        @Override
        protected void update(TeleportAction action) {
            this.xInput.update(action.target.x());
            this.yInput.update(action.target.y());
            this.zInput.update(action.target.z());
            this.yawInput.update(action.target.yaw());
            this.pitchInput.update(action.target.pitch());

            commandButton.lorePostfix(host.hasTag(TeleportAction.SPC_TAG)
                    ? LORE_POSTFIX_CLICKEDIT : LORE_POSTFIX_NOT_AVAILABLE);
        }

        private void beginCommandUpdate() {
            var spcTarget = host.getTag(TeleportAction.SPC_TAG);
            if (spcTarget == null) return;

            host.player().sendMessage(Component.translatable("command.set_precise_coords.begin"));
            host.player().setTag(TeleportAction.SPC_TAG, spcTarget);
            host.player().closeInventory();
        }

        private Consumer<String> safeUpdate(BiFunction<RelativePos, String, RelativePos> updateFunc) {
            return update((data, value) -> {
                try {
                    return new TeleportAction(updateFunc.apply(data.target, value));
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
        private final String anvilTitle;

        private String value = "";

        public TexturelessNumberInput(int width, int xOffset, String label, Consumer<String> onChange) {
            super(width, 2);
            this.onChange = onChange;

            var translationKey = "gui.action.teleport." + label;
            this.anvilTitle = LanguageProviderV2.translateToPlain(translationKey + ".name");
            add(0, 0, new Text(translationKey, width, 1, anvilTitle)
                    .font("small").align(1 + xOffset, 6));
            this.inputText = add(0, 1, new Text(translationKey, width, 1, "")
                    .align(xOffset + 6, 5));
            this.inputText.onLeftClick(this::handleEditValue);
        }

        public void update(String value) {
            if (RELATIVE_ZERO.equals(value)) value = "";

            this.value = value;
            this.inputText.text(normalizeForDisplay(value));
        }

        private void handleEditValue() {
            host.pushView(simpleAnvil(
                    "generic2/anvil/field_container",
                    "action/anvil/teleport_icon",
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
