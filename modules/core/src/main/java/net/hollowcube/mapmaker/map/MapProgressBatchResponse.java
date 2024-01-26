package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MapProgressBatchResponse {
    private List<Entry> progress = new ArrayList<>();
    private transient Map<String, PersonalizedMapData.Progress> progressByMap;

    public MapProgressBatchResponse() {
        // Gson constructor
    }

    public MapProgressBatchResponse(List<Entry> progress) {
        this.progress = progress;
    }

    public List<Entry> progress() {
        return progress;
    }

    public PersonalizedMapData.@UnknownNullability Progress getProgress(@NotNull String mapId) {
        if (progressByMap == null) {
            progressByMap = progress.stream().collect(Collectors.toMap(Entry::mapId, Entry::progress));
        }

        return progressByMap.get(mapId);
    }

    public record Entry(@NotNull String mapId, @NotNull PersonalizedMapData.Progress progress) {

    }
}
