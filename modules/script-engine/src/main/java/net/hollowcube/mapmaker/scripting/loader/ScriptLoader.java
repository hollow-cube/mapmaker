package net.hollowcube.mapmaker.scripting.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

/**
 * Script loader is responsible for loading and (if supported) hot reloading scripts.
 */
public interface ScriptLoader extends Closeable {

    @Nullable String load(@NotNull URI uri) throws IOException;

    default void close() throws IOException {
    }

    interface ReloadHook {
        void onReload(@NotNull URI uri, @Nullable String code);
    }

}
