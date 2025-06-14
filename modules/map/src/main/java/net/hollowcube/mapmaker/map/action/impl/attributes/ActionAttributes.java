package net.hollowcube.mapmaker.map.action.impl.attributes;

import net.hollowcube.mapmaker.panels.Sprite;
import net.minestom.server.entity.attribute.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ActionAttributes {

    public static final Map<Attribute, Entry> ENTRIES = new HashMap<>();
    static {
        register(Attribute.SCALE, 3, 3);
        register(Attribute.BLOCK_INTERACTION_RANGE, 3, 3);
        register(Attribute.STEP_HEIGHT, 4, 4);
        register(Attribute.GRAVITY, 2, 2, 2, 0);
    }

    private static void register(@NotNull Attribute attribute, int offsetX, int offsetY, int secondaryOffsetX, int secondaryOffsetY) {
        var entry = Entry.of(attribute, offsetX, offsetY, secondaryOffsetX, secondaryOffsetY);
        ENTRIES.put(attribute, entry);
    }

    private static void register(@NotNull Attribute attribute, int offsetX, int offsetY) {
        register(attribute, offsetX, offsetY, offsetX, offsetY);
    }

    public record Entry(
            @NotNull Attribute attribute,
            @NotNull Sprite sprite,
            @NotNull Sprite setSprite,
            @NotNull Sprite addSprite,
            @NotNull Sprite subSprite
    ) {

        public String id() {
            return attribute.key().value();
        }

        private static Entry of(@NotNull Attribute attribute, int offsetX, int offsetY, int secondaryOffsetX, int secondaryOffsetY) {
            var id = attribute.key().value();
            return new Entry(
                    attribute,
                    new Sprite("action/icon/attribute/" + id, offsetX, offsetY),
                    new Sprite("action/icon/attribute/" + id + "_set", secondaryOffsetX, secondaryOffsetY),
                    new Sprite("action/icon/attribute/" + id + "_add", secondaryOffsetX, secondaryOffsetY),
                    new Sprite("action/icon/attribute/" + id + "_sub", secondaryOffsetX, secondaryOffsetY)
            );
        }
    }
}
