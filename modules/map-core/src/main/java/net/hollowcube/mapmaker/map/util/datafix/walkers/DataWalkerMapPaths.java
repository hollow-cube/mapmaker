package net.hollowcube.mapmaker.map.util.datafix.walkers;

import ca.spottedleaf.dataconverter.converters.datatypes.DataType;
import ca.spottedleaf.dataconverter.converters.datatypes.DataWalker;
import ca.spottedleaf.dataconverter.types.MapType;

public class DataWalkerMapPaths<T, R> implements DataWalker<MapType<String>> {

    protected final DataType<T, R> type;
    protected final String[] paths;

    public DataWalkerMapPaths(final DataType<T, R> type, final String... paths) {
        this.type = type;
        this.paths = paths;
    }

    @Override
    public MapType<String> walk(MapType<String> data, long fromVersion, long toVersion) {
        final DataType<T, R> type = this.type;
        for (final String path : this.paths) {
            final MapType<String> map = data.getMap(path);
            if (map == null) {
                continue;
            }

            for (final String key : map.keys()) {
                final Object current = map.getGeneric(key);
                final Object converted = type.convert((T) current, fromVersion, toVersion);
                if (converted != null) {
                    map.setGeneric(key, converted);
                }
            }
        }

        return null;
    }
}
