package net.hollowcube.mapmaker.util.gson;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.player.DisplayName;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class DisplayNameTypeAdapter implements JsonSerializer<DisplayName>, JsonDeserializer<DisplayName> {

    @Override
    public JsonElement serialize(DisplayName displayName, Type type, JsonSerializationContext jsonSerializationContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DisplayName deserialize(JsonElement elem, Type type, JsonDeserializationContext context) throws JsonParseException {
        Type listOfPartType = new TypeToken<ArrayList<DisplayName.Part>>() {
        }.getType();
        return new DisplayName(context.deserialize(elem, listOfPartType));
    }

}
