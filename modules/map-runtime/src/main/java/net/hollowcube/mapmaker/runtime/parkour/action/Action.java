package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

public interface Action {
    StructCodec<? extends Action> codec();

    default void applyTo(Player player, PlayState state) {
    }

    enum Type {
        SPAWN("Spawn"),
        CHECKPOINT("Checkpoint"),
        STATUS("Status");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    record Editor<T extends Action>(
            @Nullable Function<ActionList.Ref, @Nullable Panel> editor,
            Function<@Nullable T, Sprite> sprite,
            @Nullable Function<@Nullable T, TranslatableComponent> thumbnail,
            Set<Key> exclusiveSet,
            @MagicConstant(valuesFromClass = Permission.class) long requiredPermission
    ) {
        private static final Sprite DEFAULT_SPRITE = new Sprite("map_details/action/report", 6, 3);

        public Editor(@Nullable Function<ActionList.Ref, @Nullable Panel> editor) {
            this(editor, _ -> DEFAULT_SPRITE, null, Set.of(), Permission.NONE);
        }

        public Editor(@Nullable Function<ActionList.Ref, @Nullable Panel> editor,
                      Sprite sprite) {
            this(editor, _ -> sprite, null, Set.of(), Permission.NONE);
        }

        public Editor(@Nullable Function<ActionList.Ref, @Nullable Panel> editor,
                      Function<@Nullable T, Sprite> sprite) {
            this(editor, sprite, null, Set.of(), Permission.NONE);
        }

        public Editor(@Nullable Function<ActionList.Ref, @Nullable Panel> editor,
                      Sprite sprite,
                      TranslatableComponent thumbnail) {
            this(editor, _ -> sprite, _ -> thumbnail, Set.of(), Permission.NONE);
        }

        public Editor(@Nullable Function<ActionList.Ref, @Nullable Panel> editor,
                      Sprite sprite, @Nullable Function<@Nullable T, TranslatableComponent> thumbnail) {
            this(editor, _ -> sprite, thumbnail, Set.of(), Permission.NONE);
        }

        public Editor(@Nullable Function<ActionList.Ref, @Nullable Panel> editor,
                      Function<@Nullable T, Sprite> sprite,
                      @Nullable Function<@Nullable T, TranslatableComponent> thumbnail,
                      Set<Key> exclusiveSet) {
            this(editor, sprite, thumbnail, exclusiveSet, Permission.NONE);
        }

    }
}
