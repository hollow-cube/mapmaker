package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.gui.ControlledTriStateInput;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class EditLivesAction extends AbstractAction<EditLivesAction.Data> {
    public static final EditLivesAction INSTANCE = new EditLivesAction();

    private static final int DEFAULT_LIVES = 0; // Disables the lives mechanic.

    public enum Operation {
        SET, ADD, SUBTRACT;

        public static final Codec<Operation> CODEC = Codec.Enum(Operation.class);
    }

    public record Data(@NotNull EditLivesAction.Operation operation, int lives) {
        public static final StructCodec<Data> CODEC = StructCodec.struct(
                "operation", Operation.CODEC.optional(Operation.SET), Data::operation,
                "value", ExtraCodecs.clamppedInt(0, 10).optional(DEFAULT_LIVES), Data::lives,
                Data::new);

        public @NotNull Data withOperation(@NotNull Operation operation) {
            return new Data(operation, this.lives);
        }

        public @NotNull Data withLives(int lives) {
            return new Data(this.operation, lives);
        }
    }

    public EditLivesAction() {
        super("mapmaker:lives", Data.CODEC, new Data(Operation.SET, DEFAULT_LIVES));
    }

    @Override
    public @NotNull AbstractActionEditorPanel<Data> createEditor() {
        return new Editor();
    }

    private static class Editor extends AbstractActionEditorPanel<Data> {

        private final ControlledTriStateInput<Operation> operationInput;
        private final ControlledNumberInput valueInput;

        public Editor() {
            super(INSTANCE);

            this.operationInput = add(1, 1, new ControlledTriStateInput<>(Operation.class, update(Data::withOperation))
                    .labels("Set", "Add", "Sub.")
                    .sprites("action/icon/lives_set", 1, 3,
                            "action/icon/lives_add", 1, 3,
                            "action/icon/lives_subtract", 1, 3));
            this.valueInput = add(1, 3, new ControlledNumberInput(update(Data::withLives))
                    .range(0, 20).formatted(i -> i == 0 ? "Disable" : String.valueOf(i)));
        }

        @Override
        protected void update(@NotNull Data data) {
            var operationName = data.operation().name().toLowerCase(Locale.ROOT);

            this.subtitleText.text(translate("subtitle." + operationName));
            this.operationInput.update(data.operation());
            this.valueInput.label("amount to " + operationName).update(data.lives);
        }
    }
}
