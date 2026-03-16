package net.hollowcube.mapmaker.map;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class PersonalizedMapData extends MapData {

    public enum Progress {
        @SerializedName("none") NONE,
        @SerializedName("started") STARTED,
        @SerializedName("complete") COMPLETE
    }

    private Progress progress;

    public PersonalizedMapData() {
        super();
    }

    public PersonalizedMapData(String id, String owner, MapSettings settings, long publishedId, @Nullable Instant publishedAt) {
        super(id, owner, settings, publishedId, publishedAt);
    }

    public PersonalizedMapData(MapData map, Progress progress) {
        super(map.id(), map.owner(), map.settings(), map.publishedId(), map.publishedAt());
        this.progress = progress;
    }

}
