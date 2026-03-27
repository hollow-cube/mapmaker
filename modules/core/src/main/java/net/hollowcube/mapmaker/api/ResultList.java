package net.hollowcube.mapmaker.api;

import net.hollowcube.common.util.RuntimeGson;

import java.util.Iterator;
import java.util.List;

@RuntimeGson
public record ResultList<T>(List<T> results) implements Iterable<T> {

    @Override
    public Iterator<T> iterator() {
        return results.iterator();
    }
}
