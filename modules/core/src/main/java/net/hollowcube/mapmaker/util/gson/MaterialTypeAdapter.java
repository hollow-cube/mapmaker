package net.hollowcube.mapmaker.util.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minestom.server.item.Material;

import java.io.IOException;

public class MaterialTypeAdapter extends TypeAdapter<Material> {
    @Override
    public void write(JsonWriter out, Material value) throws IOException {
        out.value(value.name());
    }

    @Override
    public Material read(JsonReader in) throws IOException {
        return Material.fromKey(in.nextString());
    }
}
