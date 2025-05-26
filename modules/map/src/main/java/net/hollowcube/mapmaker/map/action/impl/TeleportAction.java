package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.util.RelativePos;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.panels.Text;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

public record TeleportAction(
        @NotNull RelativePos target
) implements Action {
    private static final Sprite SPRITE_DEFAULT = new Sprite("action/icon/teleport", 3, 3);
    private static final String RELATIVE_ZERO = "~0.0";
    private static final Sound TELEPORT_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_TELEPORT, Sound.Source.PLAYER, 0.5f, 1f);

    public static final Tag<Object> SPC_TAG = Tag.Transient("mapmaker:spc/tag");

    public static final Key KEY = Key.key("mapmaker:teleport");
    public static final StructCodec<TeleportAction> CODEC = StructCodec.struct(
            StructCodec.INLINE, RelativePos.CODEC.optional(RelativePos.REL_ZERO), TeleportAction::target,
            TeleportAction::new);
    public static final Action.Editor<TeleportAction> EDITOR = new Action.Editor<>(
            TeleportAction.Editor::new, _ -> SPRITE_DEFAULT,
            TeleportAction::makeThumbnail, Set.of(KEY));

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        player.teleport(target.inner(), Vec.ZERO, null, target.relativeFlags());
        player.playSound(TELEPORT_SOUND, target.resolve(player.getPosition()));
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable TeleportAction action) {
        if (action == null) return Component.translatable("gui.action.teleport.thumbnail.empty");
        return Component.translatable("gui.action.teleport.thumbnail", List.of(
                tildeOnly(action.target.strX()), tildeOnly(action.target.strY()), tildeOnly(action.target.strZ()),
                tildeOnly(action.target.strYaw()), tildeOnly(action.target.strPitch())
        ));
    }

    private static @NotNull Component tildeOnly(@NotNull String value) {
        return Component.text(RELATIVE_ZERO.equals(value) ? "~" : value);
    }

    private static class Editor extends AbstractActionEditorPanel<TeleportAction> {
        private final TexturelessNumberInput xInput;
        private final TexturelessNumberInput yInput;
        private final TexturelessNumberInput zInput;
        private final TexturelessNumberInput yawInput;
        private final TexturelessNumberInput pitchInput;
        private final Button commandButton;

        public Editor(@NotNull ActionList.Ref ref) {
            super(ref);

            background("action/editor/teleport_container", -10, -31);

            subtitleText.text("Set Coords");

            this.xInput = add(1, 1, new TexturelessNumberInput(2, 0, "x", safeUpdate(RelativePos::withStrX)));
            this.yInput = add(3, 1, new TexturelessNumberInput(2, 6, "y", safeUpdate(RelativePos::withStrY)));
            this.zInput = add(5, 1, new TexturelessNumberInput(3, 12, "z", safeUpdate(RelativePos::withStrZ)));

            this.yawInput = add(1, 3, new TexturelessNumberInput(2, 0, "yaw", safeUpdate(RelativePos::withStrYaw)));
            this.pitchInput = add(3, 3, new TexturelessNumberInput(2, 6, "pitch", safeUpdate(RelativePos::withStrPitch)));
            this.commandButton = add(5, 4, new Button("gui.action.teleport.command", 3, 1)
                    .onLeftClick(this::beginCommandUpdate));
        }

        @Override
        protected void update(@NotNull TeleportAction action) {
            this.xInput.update(action.target.strX());
            this.yInput.update(action.target.strY());
            this.zInput.update(action.target.strZ());
            this.yawInput.update(action.target.strYaw());
            this.pitchInput.update(action.target.strPitch());

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

        private @NotNull Consumer<String> safeUpdate(@NotNull BiFunction<RelativePos, String, RelativePos> updateFunc) {
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

        public TexturelessNumberInput(int width, int xOffset, String label, @NotNull Consumer<String> onChange) {
            super(width, 2);
            this.onChange = onChange;

            var translationKey = "gui.action.teleport." + label + ".name";
            this.anvilTitle = LanguageProviderV2.translateToPlain(translationKey);
            add(0, 0, new Text(translationKey, width, 1, anvilTitle)
                    .font("small").align(1 + xOffset, 6));
            this.inputText = add(0, 1, new Text(translationKey, width, 1, "")
                    .align(xOffset + 6, 5));
            this.inputText.onLeftClick(this::handleEditValue);
        }

        public void update(@NotNull String value) {
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

        private @NotNull String normalizeForDisplay(@NotNull String value) {
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
