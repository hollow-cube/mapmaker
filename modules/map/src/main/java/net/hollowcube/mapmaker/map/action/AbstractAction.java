package net.hollowcube.mapmaker.map.action;

import net.kyori.adventure.key.Key;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

public class AbstractAction<T> {
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

    public @NotNull AbstractActionEditorPanel<T> createEditor() {
        return null;
    }

}
