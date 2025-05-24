package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.gui.ControlledTriStateInput;
import net.hollowcube.mapmaker.map.action.util.Operation;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/// Action for the timer feature
/// The time value is always tracked in ticks, not wall clock time.
public record EditTimerAction(
        @NotNull Operation operation,
        int time
) implements Action {
    private static final int NO_TIMER = 0; // Disables the timer mechanic.
    private static final int MAX_TIMER = 24 * 60 * 60 * 20; // 24 hours in ticks.

    private static final Sprite SPRITE_DEFAULT = new Sprite("action/icon/timer", 4, 3);
    private static final Sprite SPRITE_SET = new Sprite("action/icon/timer_set", 4, 3);
    private static final Sprite SPRITE_ADD = new Sprite("action/icon/timer_add", 4, 3);
    private static final Sprite SPRITE_SUBTRACT = new Sprite("action/icon/timer_subtract", 4, 3);

    public static final StructCodec<EditTimerAction> CODEC = StructCodec.struct(
            "operation", Operation.CODEC.optional(Operation.SET), EditTimerAction::operation,
            "value", ExtraCodecs.clamppedInt(0, 10).optional(NO_TIMER), EditTimerAction::time,
            EditTimerAction::new);
    public static final Action.Editor<EditTimerAction> EDITOR = new Action.Editor<>(
            EditTimerAction.Editor::new, EditTimerAction::makeSprite, EditTimerAction::makeThumbnail);

    public @NotNull EditTimerAction withOperation(@NotNull Operation operation) {
        return new EditTimerAction(operation, this.time);
    }

    public @NotNull EditTimerAction withLives(int lives) {
        return new EditTimerAction(this.operation, lives);
    }

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    private static @NotNull Sprite makeSprite(@Nullable EditTimerAction action) {
        if (action == null) return SPRITE_DEFAULT;
        return switch (action.operation) {
            case SET -> SPRITE_SET;
            case ADD -> SPRITE_ADD;
            case SUBTRACT -> SPRITE_SUBTRACT;
        };
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable EditTimerAction action) {
        if (action == null) return Component.translatable("gui.action.timer.thumbnail.empty");
        return Component.translatable("gui.action.timer.thumbnail", List.of(
                // todo add operation.translationKey which does this
                Component.translatable("gui.action.timer." + action.operation.name().toLowerCase(Locale.ROOT) + ".label"),
                Component.text(action.time == 0 ? "Disable Timer" : NumberUtil.formatDuration(action.time * 50L))
        ));
    }

    private static class Editor extends AbstractActionEditorPanel<EditTimerAction> {

        private final ControlledTriStateInput<Operation> operationInput;
        private final ControlledNumberInput valueInput;

        public Editor(@NotNull ActionList.Ref ref) {
            super(ref);

            this.operationInput = add(1, 1, new ControlledTriStateInput<>("timer", Operation.class, update(EditTimerAction::withOperation))
                    .label("operation").labels("set", "add", "subtract")
                    .sprites(SPRITE_SET.withOffset(2, 3),
                            SPRITE_ADD.withOffset(2, 3),
                            SPRITE_SUBTRACT.withOffset(2, 3)));
            this.valueInput = add(1, 3, new ControlledNumberInput("timer.value", update(EditTimerAction::withLives))
                    .formatted(i -> i == 0 ? "Disable Timer" : NumberUtil.formatDuration(i * 50L)) // Ticks to milliseconds
                    .parsed(i -> String.valueOf(i / 50.0), s -> (int) Float.parseFloat(s) * 20) // Seconds to ticks
                    .stepped(20, 5 * 20).range(NO_TIMER, MAX_TIMER));
        }

        @Override
        protected void update(@NotNull EditTimerAction data) {
            var operationName = data.operation().name().toLowerCase(Locale.ROOT);

            this.subtitleText.text(translate("subtitle." + operationName));
            this.operationInput.update(data.operation());
            this.valueInput.label(operationName).update(data.time);
        }
    }
}
