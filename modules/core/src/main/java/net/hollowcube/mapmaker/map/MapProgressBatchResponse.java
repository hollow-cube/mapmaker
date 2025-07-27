package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RuntimeGson
public final class MapProgressBatchResponse {
    private List<Entry> results = new ArrayList<>();
    private transient Map<String, Map.Entry<PersonalizedMapData.Progress, Integer>> progressByMap;

    public List<Entry> progress() {
        return results;
    }

    public Map.Entry<PersonalizedMapData.Progress, Integer> getProgress(@NotNull String mapId) {
        if (progressByMap == null) {
            progressByMap = results.stream()
                    .collect(Collectors.toMap(Entry::mapId, e -> Map.entry(e.progress, e.playtime)));
        }

        return progressByMap.get(mapId);
    }

    @RuntimeGson
    public record Entry(@NotNull String mapId, @NotNull PersonalizedMapData.Progress progress, int playtime) {

    }
}
