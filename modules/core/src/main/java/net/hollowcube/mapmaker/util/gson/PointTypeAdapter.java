package net.hollowcube.mapmaker.util.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

import java.io.IOException;

public class PointTypeAdapter extends TypeAdapter<Point> {
    @Override
    public void write(JsonWriter out, Point value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.beginObject();
        out.name("x").value(value.x());
        out.name("y").value(value.y());
        out.name("z").value(value.z());
        out.endObject();
    }

    @Override
    public Point read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        in.beginObject();
        double x = 0;
        double y = 0;
        double z = 0;
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "x":
                    x = in.nextDouble();
                    break;
                case "y":
                    y = in.nextDouble();
                    break;
                case "z":
                    z = in.nextDouble();
                    break;
            }
        }
        in.endObject();
        return new Vec(x, y, z);
    }
}
