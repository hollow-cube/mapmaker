package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.gui.ControlledTriStateInput;
import net.hollowcube.mapmaker.map.action.util.Operation;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public record EditLivesAction(
        @NotNull Operation operation,
        int lives
) implements Action {
    private static final int DEFAULT_LIVES = 0; // Disables the lives mechanic.

    private static final Sprite SPRITE_DEFAULT = new Sprite("action/icon/lives", 3, 3);
    private static final Sprite SPRITE_SET = new Sprite("action/icon/lives_set", 3, 3);
    private static final Sprite SPRITE_ADD = new Sprite("action/icon/lives_add", 3, 3);
    private static final Sprite SPRITE_SUBTRACT = new Sprite("action/icon/lives_subtract", 3, 3);

    public static final Key KEY = Key.key("mapmaker:lives");
    public static final StructCodec<EditLivesAction> CODEC = StructCodec.struct(
            "operation", Operation.CODEC.optional(Operation.SET), EditLivesAction::operation,
            "value", ExtraCodecs.clamppedInt(0, 10).optional(DEFAULT_LIVES), EditLivesAction::lives,
            EditLivesAction::new);
    public static final Action.Editor<EditLivesAction> EDITOR = new Action.Editor<>(
            Editor::new, EditLivesAction::makeSprite, EditLivesAction::makeThumbnail, Set.of());
    public static final PlayState.Attachment<Data> SAVE_DATA = PlayState.attachment(KEY, Data.CODEC);

    public record Data(int value, int max) {
        public static final Codec<Data> CODEC = StructCodec.struct(
                "value", ExtraCodecs.clamppedInt(0, 20), Data::value,
                "max", ExtraCodecs.clamppedInt(0, 20), Data::max,
                Data::new);

        public @NotNull Data withValue(int value) {
            return new Data(value, this.max);
        }

        public @NotNull Data withMax(int max) {
            return new Data(this.value, max);
        }
    }

    public @NotNull EditLivesAction withOperation(@NotNull Operation operation) {
        return new EditLivesAction(operation, this.lives);
    }

    public @NotNull EditLivesAction withLives(int lives) {
        return new EditLivesAction(this.operation, lives);
    }

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        switch (this.operation) {
            case SET -> state.set(EditLivesAction.SAVE_DATA, this.lives == 0
                    ? null : new EditLivesAction.Data(lives, lives));
            case ADD -> {
                var data = state.get(SAVE_DATA);
                if (data == null) state.set(SAVE_DATA, new EditLivesAction.Data(lives, lives));
                else state.set(SAVE_DATA, data.withValue(Math.min(data.value() + lives, data.max + lives)));
            }
            case SUBTRACT -> {
                var data = state.get(SAVE_DATA);
                if (data != null) state.set(SAVE_DATA, data.withValue(Math.max(data.value() - lives, 0)));
            }
        }
    }

    private static @NotNull Sprite makeSprite(@Nullable EditLivesAction action) {
        if (action == null) return SPRITE_DEFAULT;
        return switch (action.operation) {
            case SET -> SPRITE_SET;
            case ADD -> SPRITE_ADD;
            case SUBTRACT -> SPRITE_SUBTRACT;
        };
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable EditLivesAction action) {
        if (action == null) return Component.translatable("gui.action.lives.thumbnail.empty");
        return Component.translatable("gui.action.lives.thumbnail", List.of(
                Component.translatable("gui.action.lives." + action.operation.name().toLowerCase(Locale.ROOT) + ".label"),
                Component.text(action.lives == 0 ? (action.operation == Operation.SET ? "Disable" : "-") : String.valueOf(action.lives))
        ));
    }

    private static class Editor extends AbstractActionEditorPanel<EditLivesAction> {
        private final ControlledTriStateInput<Operation> operationInput;
        private final ControlledNumberInput valueInput;

        public Editor(@NotNull ActionList.Ref ref) {
            super(ref);

            this.operationInput = add(1, 1, new ControlledTriStateInput<>("lives", Operation.class, update(EditLivesAction::withOperation))
                    .label("operation").labels("set", "add", "subtract")
                    .sprites(SPRITE_SET.withOffset(1, 3),
                            SPRITE_ADD.withOffset(1, 3),
                            SPRITE_SUBTRACT.withOffset(1, 3)));
            this.valueInput = add(1, 3, new ControlledNumberInput("lives.value", update(EditLivesAction::withLives))
                    .range(0, 20).formatted(i -> i == 0 ? "Disable" : String.valueOf(i)));
        }

        @Override
        protected void update(@NotNull EditLivesAction data) {
            var operationName = data.operation().name().toLowerCase(Locale.ROOT);

            this.subtitleText.text(translate("subtitle." + operationName));
            this.operationInput.update(data.operation());
            this.valueInput.label(operationName).update(data.lives);
        }
    }
}
