package net.hollowcube.mapmaker.editor.scripting;

import net.hollowcube.mapmaker.api.maps.MapClient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// Production [ScriptSource]: pulls map files from the backend over HTTP via
/// [MapClient].
public final class MapClientScriptSource implements ScriptSource {
    private final MapClient maps;
    private final String mapId;

    public MapClientScriptSource(MapClient maps, String mapId) {
        this.maps = maps;
        this.mapId = mapId;
    }

    @Override
    public Iterable<String> listFiles() {
        List<String> paths = new ArrayList<>();
        for (var header : maps.listMapFiles(mapId)) paths.add(header.path());
        return paths;
    }

    @Override
    public byte @Nullable [] readFile(String path) {
        return maps.getMapFile(mapId, stripLeadingSlash(path));
    }

    private static String stripLeadingSlash(String key) {
        return key.startsWith("/") ? key.substring(1) : key;
    }
}
