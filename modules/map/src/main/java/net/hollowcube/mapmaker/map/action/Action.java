package net.hollowcube.mapmaker.map.action;

import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface Action {

    @NotNull StructCodec<? extends Action> codec();

    record Editor<T extends Action>(
            @NotNull Function<ActionList.Ref, AbstractActionEditorPanel<T>> editor,
            @NotNull Function<@Nullable T, Sprite> sprite,
            @Nullable Function<@Nullable T, TranslatableComponent> thumbnail
    ) {
        private static final @NotNull Sprite DEFAULT_SPRITE = new Sprite("map_details/action/report", 6, 3);

        public Editor(@NotNull Function<ActionList.Ref, AbstractActionEditorPanel<T>> editor) {
            this(editor, _ -> DEFAULT_SPRITE, null);
        }

        public Editor(@NotNull Function<ActionList.Ref, AbstractActionEditorPanel<T>> editor,
                      @NotNull Sprite sprite) {
            this(editor, _ -> sprite, null);
        }

        public Editor(@NotNull Function<ActionList.Ref, AbstractActionEditorPanel<T>> editor,
                      @NotNull Function<@Nullable T, Sprite> sprite) {
            this(editor, sprite, null);
        }

        public Editor(@NotNull Function<ActionList.Ref, AbstractActionEditorPanel<T>> editor,
                      @NotNull Sprite sprite,
                      @NotNull TranslatableComponent thumbnail) {
            this(editor, _ -> sprite, _ -> thumbnail);
        }

        public Editor(@NotNull Function<ActionList.Ref, AbstractActionEditorPanel<T>> editor,
                      @NotNull Sprite sprite, @Nullable Function<@Nullable T, TranslatableComponent> thumbnail) {
            this(editor, _ -> sprite, thumbnail);
        }


    }
}
