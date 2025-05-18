package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.gui.ControlledTriStateInput;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/// Action for the timer feature
/// The time value is always tracked in ticks, not wall clock time.
public class EditTimerAction extends AbstractAction<EditTimerAction.Data> {
    public static final EditTimerAction INSTANCE = new EditTimerAction();

    private static final int NO_TIMER = 0; // Disables the timer mechanic.
    private static final int MAX_TIMER = 24 * 60 * 60 * 20; // 24 hours in ticks.

    private static final Sprite SPRITE_DEFAULT = new Sprite("action/icon/timer", 4, 3);
    private static final Sprite SPRITE_SET = new Sprite("action/icon/timer_set", 4, 3);
    private static final Sprite SPRITE_ADD = new Sprite("action/icon/timer_add", 4, 3);
    private static final Sprite SPRITE_SUBTRACT = new Sprite("action/icon/timer_subtract", 4, 3);

    public enum Operation {
        SET, ADD, SUBTRACT;

        public static final Codec<Operation> CODEC = Codec.Enum(Operation.class);
    }

    public record Data(@NotNull EditTimerAction.Operation operation, int time) {
        public static final StructCodec<Data> CODEC = StructCodec.struct(
                "operation", Operation.CODEC.optional(Operation.SET), Data::operation,
                "value", ExtraCodecs.clamppedInt(0, 10).optional(NO_TIMER), Data::time,
                Data::new);

        public @NotNull Data withOperation(@NotNull Operation operation) {
            return new Data(operation, this.time);
        }

        public @NotNull Data withLives(int lives) {
            return new Data(this.operation, lives);
        }
    }

    public EditTimerAction() {
        super("mapmaker:timer", Data.CODEC, new Data(Operation.SET, NO_TIMER));
    }

    @Override
    public @NotNull Sprite sprite(@Nullable Data data) {
        if (data == null) return SPRITE_DEFAULT;
        return switch (data.operation) {
            case SET -> SPRITE_SET;
            case ADD -> SPRITE_ADD;
            case SUBTRACT -> SPRITE_SUBTRACT;
        };
    }

    @Override
    public @NotNull TranslatableComponent thumbnail(@Nullable Data data) {
        if (data == null) return Component.translatable("gui.action.timer.thumbnail.empty");
        return Component.translatable("gui.action.timer.thumbnail", List.of(
                Component.translatable("gui.action.timer." + data.operation().name().toLowerCase(Locale.ROOT) + ".label"),
                Component.text(data.time() == 0 ? "Disable Timer" : NumberUtil.formatDuration(data.time() * 50L))
        ));
    }

    @Override
    public @NotNull AbstractActionEditorPanel<Data> createEditor(@NotNull ActionList.ActionData<Data> actionData) {
        return new Editor(actionData);
    }

    private static class Editor extends AbstractActionEditorPanel<Data> {

        private final ControlledTriStateInput<Operation> operationInput;
        private final ControlledNumberInput valueInput;

        public Editor(@NotNull ActionList.ActionData<Data> actionData) {
            super(actionData);

            this.operationInput = add(1, 1, new ControlledTriStateInput<>("timer", Operation.class, update(Data::withOperation))
                    .label("operation").labels("set", "add", "subtract")
                    .sprites(SPRITE_SET.withOffset(2, 3),
                            SPRITE_ADD.withOffset(2, 3),
                            SPRITE_SUBTRACT.withOffset(2, 3)));
            this.valueInput = add(1, 3, new ControlledNumberInput("timer.value", update(Data::withLives))
                    .formatted(i -> i == 0 ? "Disable Timer" : NumberUtil.formatDuration(i * 50L)) // Ticks to milliseconds
                    .parsed(i -> String.valueOf(i / 50.0), s -> (int) Float.parseFloat(s) * 20) // Seconds to ticks
                    .stepped(20, 5 * 20).range(NO_TIMER, MAX_TIMER));
        }

        @Override
        protected void update(@NotNull Data data) {
            var operationName = data.operation().name().toLowerCase(Locale.ROOT);

            this.subtitleText.text(translate("subtitle." + operationName));
            this.operationInput.update(data.operation());
            this.valueInput.label(operationName).update(data.time);
        }
    }
}
