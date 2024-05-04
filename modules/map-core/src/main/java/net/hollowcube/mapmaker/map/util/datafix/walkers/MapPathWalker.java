package net.hollowcube.mapmaker.map.util.datafix.walkers;

import ca.spottedleaf.dataconverter.converters.datatypes.DataWalker;
import ca.spottedleaf.dataconverter.types.MapType;
import org.jetbrains.annotations.NotNull;

public class MapPathWalker implements DataWalker<MapType<String>> {
    private final String path;
    private final DataWalker<MapType<String>> child;

    public MapPathWalker(@NotNull String path, @NotNull DataWalker<MapType<String>> child) {
        this.path = path;
        this.child = child;
    }

    @Override
    public MapType<String> walk(MapType<String> data, long fromVersion, long toVersion) {
        MapType<String> map = data.getMap(path);
        if (map == null) {
            return null;
        }

        child.walk(map, fromVersion, toVersion);

        return null;
    }
}
