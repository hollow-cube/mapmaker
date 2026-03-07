package net.hollowcube.mapmaker.api;

import net.hollowcube.common.util.RuntimeGson;

import java.util.List;

@RuntimeGson
public record ResultList<T>(List<T> results) {
}
