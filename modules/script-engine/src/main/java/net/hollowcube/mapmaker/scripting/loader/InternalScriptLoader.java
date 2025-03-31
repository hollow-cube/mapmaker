package net.hollowcube.mapmaker.scripting.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Exists to load some internal scripts from inside the jar always. Never supports hot reload.
 */
public class InternalScriptLoader implements ScriptLoader {
    private static final Map<String, String> STATIC_REMAPPINGS = Map.of(
            "/react/react-refresh/runtime.js", "/react/react-refresh-runtime.js"
    );

    private final String prefix;

    public InternalScriptLoader(@NotNull String prefix) {
        this.prefix = prefix;
    }

    @Override
    public @Nullable String load(@NotNull URI uri) throws IOException {
        final String path = STATIC_REMAPPINGS.getOrDefault(uri.getPath(), uri.getPath());

        try (var is = getClass().getResourceAsStream(prefix + path)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
