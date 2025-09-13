package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

public interface Action {
    StructCodec<? extends Action> codec();

    default void applyTo(Player player, PlayState state) {
    }

    enum Type {
        SPAWN,
        CHECKPOINT,
        STATUS,
    }

    record Editor<T extends Action>(
            @Nullable Function<ActionList.Ref, Panel> editor,
            Function<@Nullable T, Sprite> sprite,
            @Nullable Function<@Nullable T, TranslatableComponent> thumbnail,
            Set<Key> exclusiveSet
    ) {
        private static final Sprite DEFAULT_SPRITE = new Sprite("map_details/action/report", 6, 3);

        public Editor(@Nullable Function<ActionList.Ref, Panel> editor) {
            this(editor, _ -> DEFAULT_SPRITE, null, Set.of());
        }

        public Editor(@Nullable Function<ActionList.Ref, Panel> editor,
                      Sprite sprite) {
            this(editor, _ -> sprite, null, Set.of());
        }

        public Editor(@Nullable Function<ActionList.Ref, Panel> editor,
                      Function<@Nullable T, Sprite> sprite) {
            this(editor, sprite, null, Set.of());
        }

        public Editor(@Nullable Function<ActionList.Ref, Panel> editor,
                      Sprite sprite,
                      TranslatableComponent thumbnail) {
            this(editor, _ -> sprite, _ -> thumbnail, Set.of());
        }

        public Editor(@Nullable Function<ActionList.Ref, Panel> editor,
                      Sprite sprite, @Nullable Function<@Nullable T, TranslatableComponent> thumbnail) {
            this(editor, _ -> sprite, thumbnail, Set.of());
        }


    }
}
