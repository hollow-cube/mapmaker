package net.hollowcube.mapmaker.hub.feature.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.player.PlayerSetting;

public record EventData(IntSet presents) {

    public static final EventData EMPTY = new EventData(IntSet.of());
    public static final PlayerSetting<EventData> SETTING = PlayerSetting.create(
            "hub.event",
            EMPTY,
            EventData::toJson,
            EventData::fromJson
    );
    private static final String KEY = "christmas_2025";

    public boolean hasPresent(int day) {
        return presents.contains(day);
    }

    public EventData withPresent(int day) {
        var newPresents = new IntArraySet(presents);
        newPresents.add(day);
        return new EventData(newPresents);
    }

    private static EventData fromJson(JsonElement element) {
        if (!element.isJsonObject()) return EMPTY;
        JsonObject json = element.getAsJsonObject();
        if (!json.has(KEY)) return EMPTY;
        var presents = new IntArraySet();
        if (json.has("presents") && json.get("presents").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("presents");
            for (int i = 0; i < array.size(); i++) {
                presents.add(array.get(i).getAsInt());
            }
        }
        return new EventData(presents);
    }

    private static JsonElement toJson(EventData data) {
        JsonObject json = new JsonObject();
        json.addProperty(KEY, true);
        json.add("presents", OpUtils.build(new JsonArray(), array -> {
            for (int present : data.presents) {
                array.add(present);
            }
        }));
        return json;
    }
}
