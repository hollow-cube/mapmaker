package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RuntimeGson
public final class MapProgressBatchResponse {
    private List<Entry> progress = new ArrayList<>();
    private transient Map<String, Map.Entry<PersonalizedMapData.Progress, Integer>> progressByMap;

    public MapProgressBatchResponse() {
        // Gson constructor
    }

    public MapProgressBatchResponse(List<Entry> progress) {
        this.progress = progress;
    }

    public List<Entry> progress() {
        return progress;
    }

    public Map.Entry<PersonalizedMapData.Progress, Integer> getProgress(@NotNull String mapId) {
        if (progressByMap == null) {
            progressByMap = progress.stream()
                    .collect(Collectors.toMap(Entry::mapId, e -> Map.entry(e.progress, e.playtime)));
        }

        return progressByMap.get(mapId);
    }

    @RuntimeGson
    public record Entry(@NotNull String mapId, @NotNull PersonalizedMapData.Progress progress, int playtime) {

    }
}
