package net.hollowcube.mapmaker.map.action;

import net.hollowcube.mapmaker.panels.Panel;
import net.kyori.adventure.key.Key;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

public class AbstractAction<T> {
    private final Key key;
    private final StructCodec<T> codec;

    protected AbstractAction(@NotNull String key, @NotNull StructCodec<T> codec) {
        this.key = Key.key(key);
        this.codec = codec;
    }

    public @NotNull Key key() {
        return key;
    }

    public @NotNull StructCodec<T> codec() {
        return codec;
    }

    public @NotNull Panel createEditor() {

    }

}
