package net.hollowcube.mapmaker.map.action;

import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

public interface Action {
    @NotNull StructCodec<? extends Action> codec();

    default void applyTo(@NotNull Player player, @NotNull PlayState state) {
    }

    record Editor<T extends Action>(
            @Nullable Function<ActionList.Ref, Panel> editor,
            @NotNull Function<@Nullable T, Sprite> sprite,
            @Nullable Function<@Nullable T, TranslatableComponent> thumbnail,
            @NotNull Set<Key> exclusiveSet
    ) {
        private static final @NotNull Sprite DEFAULT_SPRITE = new Sprite("map_details/action/report", 6, 3);

        public Editor(@Nullable Function<ActionList.Ref, Panel> editor) {
            this(editor, _ -> DEFAULT_SPRITE, null, Set.of());
        }

        public Editor(@Nullable Function<ActionList.Ref, Panel> editor,
                      @NotNull Sprite sprite) {
            this(editor, _ -> sprite, null, Set.of());
        }

        public Editor(@Nullable Function<ActionList.Ref, Panel> editor,
                      @NotNull Function<@Nullable T, Sprite> sprite) {
            this(editor, sprite, null, Set.of());
        }

        public Editor(@Nullable Function<ActionList.Ref, Panel> editor,
                      @NotNull Sprite sprite,
                      @NotNull TranslatableComponent thumbnail) {
            this(editor, _ -> sprite, _ -> thumbnail, Set.of());
        }

        public Editor(@Nullable Function<ActionList.Ref, Panel> editor,
                      @NotNull Sprite sprite, @Nullable Function<@Nullable T, TranslatableComponent> thumbnail) {
            this(editor, _ -> sprite, thumbnail, Set.of());
        }


    }
}
