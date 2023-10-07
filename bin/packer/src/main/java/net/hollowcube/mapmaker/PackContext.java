package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

    private Path resources;
    private Path out;
    private boolean minify = false;
    private int resourceId = 0;

    private Path rpMinecraftBase;
    private Path rpMapmakerBase;
    private String mapmakerRefBase;

    private final Map<String, String> remapping = new HashMap<>();

    private final JsonObject fontFile;

    private final JsonObject leatherArmorFile;
    private int leatherArmorCMD = 1;

    private JsonArray serverSprites = new JsonArray();


    public PackContext(Path resources, Path out) throws IOException {
        this.resources = resources;
        this.out = out;

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

        leatherArmorFile = new JsonObject();
        leatherArmorFile.addProperty("parent", "item/generated");
        JsonObject leatherTextures = new JsonObject();
        leatherTextures.addProperty("layer0", "item/diamond");
        leatherArmorFile.add("textures", leatherTextures);
        leatherArmorFile.add("overrides", new JsonArray());
    }

    public @NotNull Path resources() {
        return resources;
    }

    public @NotNull Path out() {
        return out;
    }

    // Resource pack methods

    /**
     * Writes a texture and returns a reference to it.
     */
    public @NotNull String writeTexture(@Nullable String type, @NotNull String name, byte[] data) {
        try {
            if (name.endsWith(".png")) name = name.substring(0, name.length() - 4);
            name = minifyId(name);

            Path path = rpMapmakerBase.resolve("textures");
            if (type != null) path = path.resolve(type);
            path = path.resolve(name + ".png");
            Files.createDirectories(path.getParent());

            Files.write(path, data);
            return mapmakerRefBase + (type == null ? "" : type + "/") + name + (type == null ? ".png" : "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull String writeBasicModel(@NotNull String name, byte[] data) throws IOException {
        name = minifyId(name);

        JsonObject obj = new JsonObject();
        obj.addProperty("parent", "item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", writeTexture("item", name, data));
        obj.add("textures", textures);

        Path path = rpMapmakerBase.resolve("models").resolve("item").resolve(name + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new Gson().toJson(obj));

        return mapmakerRefBase + "item/" + name;
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

    public int addBasicItemTexture(@NotNull String name, byte[] texture) throws IOException {
        int cmd = leatherArmorCMD++;
        JsonObject override = new JsonObject();
        JsonObject predicate = new JsonObject();
        predicate.addProperty("custom_model_data", cmd);
        override.add("predicate", predicate);
        override.addProperty("model", writeBasicModel(name, texture));
        leatherArmorFile.getAsJsonArray("overrides").add(override);
        return cmd;
    }

    public int addBasicItem(@NotNull String name, String model) throws IOException {
        int cmd = leatherArmorCMD++;
        JsonObject override = new JsonObject();
        JsonObject predicate = new JsonObject();
        predicate.addProperty("custom_model_data", cmd);
        override.add("predicate", predicate);
        override.addProperty("model", model);
        leatherArmorFile.getAsJsonArray("overrides").add(override);
        return cmd;
    }

    public JsonArray getServerSprites() {
        return serverSprites;
    }

    public void cleanup() throws IOException {
        System.out.println("Cleanup");
        Gson gson = new GsonBuilder().create();

        Files.writeString(out.resolve("mapping.json"), gson.toJson(remapping));

        Path fontFile = rpMinecraftBase.resolve("font").resolve("default.json");
        String fontDefinition = gson.toJson(this.fontFile);
        while (fontDefinition.contains("\\\\")) {
            // How do i do this with regex???
            fontDefinition = fontDefinition.replace("\\\\", "\\");
        }
        Files.writeString(fontFile, fontDefinition);

        Path leatherArmorFile = rpMinecraftBase.resolve("models").resolve("item").resolve("diamond.json");
        Files.writeString(leatherArmorFile, gson.toJson(this.leatherArmorFile));

        Path serverSpritesPath = out().resolve("server").resolve("sprites.json");
        Files.createDirectories(serverSpritesPath.getParent());
        String sprites = gson.toJson(serverSprites);
        while (sprites.contains("\\\\")) {
            // How do i do this with regex???
            sprites = sprites.replace("\\\\", "\\");
        }
        Files.writeString(serverSpritesPath, sprites);

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
                    Files.copy(file, out.resolve(resources.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                } catch (DirectoryNotEmptyException ignored) {
//                    System.out.println(e.getMessage());
                }
            }

        }
    }
}
