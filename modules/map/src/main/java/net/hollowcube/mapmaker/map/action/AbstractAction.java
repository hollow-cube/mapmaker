package net.hollowcube.mapmaker.map.action;

import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractAction<T> {
    private static final Sprite DEFAULT_SPRITE = new Sprite("map_details/action/report", 6, 3);

    private final Key key;
    private final StructCodec<T> codec;
    private final T defaultData;

    protected AbstractAction(@NotNull String key, @NotNull StructCodec<T> codec, @NotNull T defaultData) {
        this.key = Key.key(key);
        this.codec = codec;
        this.defaultData = defaultData;
    }

    public @NotNull Key key() {
        return key;
    }

    public @NotNull StructCodec<T> codec() {
        return codec;
    }

    public @NotNull T defaultData() {
        return defaultData;
    }

    public @NotNull Sprite sprite(@Nullable T data) {
        return DEFAULT_SPRITE;
    }

    public @NotNull TranslatableComponent thumbnail(@Nullable T data) {
        return Component.translatable("gui.action." + key.value() + ".thumbnail");
    }

    public abstract @NotNull AbstractActionEditorPanel<T> createEditor(@NotNull ActionList.ActionData<T> actionData);

}
