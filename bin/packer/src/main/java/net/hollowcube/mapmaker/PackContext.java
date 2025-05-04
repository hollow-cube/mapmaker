package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.type.ServerSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class PackContext {

    private final Path resources;
    private final Path out;
    private final Path minecraft;
    private boolean minify = true;
    private int resourceId = 0;

    private final Path rpMinecraftBase;
    private final Path rpMapmakerBase;
    private final String mapmakerRefBase;

    private final Map<String, String> remapping = new HashMap<>();

    private final JsonObject fontFile;

    private final JsonArray serverSprites = new JsonArray();

    public final JsonObject dynamicData = new JsonObject();


    public PackContext(Path resources, Path out, Path minecraft) throws IOException {
        this.resources = resources;
        this.out = out;
        this.minecraft = minecraft;

        copyStaticFiles();

        this.rpMinecraftBase = out.resolve("client").resolve("assets").resolve("minecraft");
        Files.createDirectories(rpMinecraftBase);

        if (minify) {
            this.rpMapmakerBase = rpMinecraftBase;
            this.mapmakerRefBase = "minecraft:";
        } else {
            this.rpMapmakerBase = out.resolve("client").resolve("assets").resolve("mapmaker");
            Files.createDirectories(rpMapmakerBase);
            this.mapmakerRefBase = "mapmaker:";
        }

        fontFile = new Gson().fromJson(Files.readString(rpMinecraftBase.resolve("font").resolve("default.json")), JsonObject.class);
    }

    public @NotNull Path resources() {
        return resources;
    }

    public @NotNull Path out() {
        return out;
    }

    public @NotNull Path vanilla() {
        return minecraft;
    }

    // Resource pack methods

    /**
     * Writes a texture and returns a reference to it.
     */
    public @NotNull String writeTexture(@Nullable String type, @NotNull String name, byte[] data, byte @Nullable [] mcmeta) {
        try {
            if (name.endsWith(".png")) name = name.substring(0, name.length() - 4);
            name = minifyId(name);

            Path path = rpMapmakerBase.resolve("textures");
            if (type != null) path = path.resolve(type);
            path = path.resolve(name + ".png");
            Files.createDirectories(path.getParent());

            Files.write(path, data);
            if (mcmeta != null) Files.write(path.resolveSibling(name + ".png.mcmeta"), mcmeta);
            return mapmakerRefBase + (type == null ? "" : type + "/") + name + (type == null ? ".png" : "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes a texture and returns a reference to it.
     */
    public @NotNull String writeTexture(@Nullable String type, @NotNull String name, byte[] data) {
        return writeTexture(type, name, data, null);
    }

    public @NotNull String writeModel(@NotNull String name, @NotNull JsonObject model) throws IOException {
        name = minifyId(name);

        Path path = rpMapmakerBase.resolve("models").resolve("item").resolve(name + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new Gson().toJson(model));

        return mapmakerRefBase + "item/" + name;
    }

    public void addFontCharacter(@NotNull JsonObject definition) {
        fontFile.getAsJsonArray("providers").add(definition);
    }

    public void addItemModel(@NotNull String name, @NotNull JsonObject model) throws IOException {
        var minName = minifyId(name);

        Path path = rpMapmakerBase.resolve("items").resolve(minName + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new Gson().toJson(model));

        var clientPath = mapmakerRefBase + minName;
        addServerSprite(new ServerSprite(name, clientPath));
    }

    public void addServerSprite(@NotNull ServerSprite sprite) {
        serverSprites.add(sprite.toJson());
    }

    public void cleanup() throws IOException {
        Gson gson = new GsonBuilder().create();

        Files.writeString(out.resolve("mapping.json"), gson.toJson(remapping));

        Path fontFile = rpMinecraftBase.resolve("font").resolve("default.json");
        String fontDefinition = gson.toJson(this.fontFile);
        while (fontDefinition.contains("\\\\")) {
            // How do i do this with regex???
            fontDefinition = fontDefinition.replace("\\\\", "\\");
        }
        Files.writeString(fontFile, fontDefinition);

        Path serverSpritesPath = out().resolve("server").resolve("sprites.json");
        Files.createDirectories(serverSpritesPath.getParent());
        String sprites = gson.toJson(serverSprites);
        while (sprites.contains("\\\\")) {
            // How do i do this with regex???
            sprites = sprites.replace("\\\\", "\\");
        }
        Files.writeString(serverSpritesPath, sprites);

        Files.writeString(out().resolve("server").resolve("dynamic.json"), gson.toJson(dynamicData));
    }

    public Path rpMinecraftBase() {
        return rpMinecraftBase;
    }

    private @NotNull String minifyId(@NotNull String id) {
        if (!minify) {
            // Need to flatten to avoid issues with atlases
            return id.replace("/", "_");
        }

        String newId = Integer.toString(resourceId++, 36);
        remapping.put(id, newId);
        return newId;
    }

    private void copyStaticFiles() throws IOException {
        try (Stream<Path> files = Files.walk(resources.resolve("client"))) {
            for (Path file : files.toList()) {
                try {
                    if (file.getFileName().toString().contains(".DS_Store")) continue;
                    Files.copy(file, out.resolve(resources.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                } catch (DirectoryNotEmptyException ignored) {
//                    System.out.println(e.getMessage());
                }
            }

        }
    }
}
