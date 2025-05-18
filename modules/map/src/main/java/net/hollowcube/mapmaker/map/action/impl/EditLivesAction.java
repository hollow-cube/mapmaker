package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.gui.ControlledTriStateInput;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class EditLivesAction extends AbstractAction<EditLivesAction.Data> {
    public static final EditLivesAction INSTANCE = new EditLivesAction();

    private static final int DEFAULT_LIVES = 0; // Disables the lives mechanic.

    private static final Sprite SPRITE_DEFAULT = new Sprite("action/icon/lives", 3, 3);
    private static final Sprite SPRITE_SET = new Sprite("action/icon/lives_set", 3, 3);
    private static final Sprite SPRITE_ADD = new Sprite("action/icon/lives_add", 3, 3);
    private static final Sprite SPRITE_SUBTRACT = new Sprite("action/icon/lives_subtract", 3, 3);

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
        if (data == null) return Component.translatable("gui.action.lives.thumbnail.empty");
        return Component.translatable("gui.action.lives.thumbnail", List.of(
                Component.translatable("gui.action.lives." + data.operation().name().toLowerCase(Locale.ROOT) + ".label"),
                Component.text(data.lives() == 0 ? "Disable" : String.valueOf(data.lives()))
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

            this.operationInput = add(1, 1, new ControlledTriStateInput<>("lives", Operation.class, update(Data::withOperation))
                    .label("operation").labels("set", "add", "subtract")
                    .sprites(SPRITE_SET.withOffset(1, 3),
                            SPRITE_ADD.withOffset(1, 3),
                            SPRITE_SUBTRACT.withOffset(1, 3)));
            this.valueInput = add(1, 3, new ControlledNumberInput("lives.value", update(Data::withLives))
                    .range(0, 20).formatted(i -> i == 0 ? "Disable" : String.valueOf(i)));
        }

        @Override
        protected void update(@NotNull Data data) {
            var operationName = data.operation().name().toLowerCase(Locale.ROOT);

            this.subtitleText.text(translate("subtitle." + operationName));
            this.operationInput.update(data.operation());
            this.valueInput.label(operationName).update(data.lives);
        }
    }
}
