package net.hollowcube.aj.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

final class JsonUtil {
    public static final float[] EMPTY_FLOATS = new float[4];

    @Contract("_, !null -> !null")
    static float[] readVec4(@Nullable JsonElement elem, float[] def) {
        if (!(elem instanceof JsonArray array)) return def;
        if (array.size() != 4) return def;
        var x = array.get(0).getAsFloat();
        var y = array.get(1).getAsFloat();
        var z = array.get(2).getAsFloat();
        var w = array.get(3).getAsFloat();
        return new float[]{x, y, z, w};
    }

    @Contract("_, !null -> !null")
    static @Nullable Point readVec3(@Nullable JsonElement elem, @Nullable Point def) {
        if (!(elem instanceof JsonArray array)) return def;
        if (array.size() != 3) return def;
        var x = array.get(0).getAsDouble();
        var y = array.get(1).getAsDouble();
        var z = array.get(2).getAsDouble();
        return new Vec(x, y, z);
    }

    @Contract("_, !null -> !null")
    static @Nullable Point readVec2(@Nullable JsonElement elem, @Nullable Point def) {
        if (!(elem instanceof JsonArray array)) return def;
        if (array.size() != 2) return def;
        var x = array.get(0).getAsDouble();
        var y = array.get(1).getAsDouble();
        return new Vec(x, y, 0);
    }

    private JsonUtil() {
    }
}
