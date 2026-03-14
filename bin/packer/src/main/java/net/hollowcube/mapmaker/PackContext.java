package net.hollowcube.mapmaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.type.ServerSprite;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
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
    private final Map<String, Path> rpMapmakerVersionsPaths = new HashMap<>();
    private final String mapmakerRefBase;

    private final Map<String, String> remapping = new HashMap<>();

    private final JsonObject fontFile;

    private final JsonArray serverSprites = new JsonArray();

    public final JsonObject dynamicData = new JsonObject();


    public PackContext(Path resources, Path out, Path minecraft, Map<String, String> mcVersions) throws IOException {
        this.resources = resources;
        this.out = out;
        this.minecraft = minecraft;

        Files.walkFileTree(out.resolve("client"), new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });

        copyStaticFiles();

        var mapmakerRef = minify ? "minecraft" : "mapmaker";
        var clientDirectory = out.resolve("client");

        this.rpMinecraftBase = clientDirectory.resolve("assets").resolve("minecraft");
        Files.createDirectories(rpMinecraftBase);

        this.rpMapmakerBase = clientDirectory.resolve("assets").resolve(mapmakerRef);
        Files.createDirectories(rpMapmakerBase);

        for (var entry : mcVersions.entrySet()) {
            var mcVersion = entry.getKey();
            var overlayPath = "pvn_%s".formatted(entry.getValue());
            var path = clientDirectory.resolve(overlayPath).resolve("assets").resolve(mapmakerRef);
            Files.createDirectories(path);

            this.rpMapmakerVersionsPaths.put(mcVersion, path);
        }

        this.mapmakerRefBase = mapmakerRef + ":";
        this.fontFile = new Gson().fromJson(Files.readString(rpMinecraftBase.resolve("font").resolve("default.json")), JsonObject.class);
    }

    public Path resources() {
        return resources;
    }

    public Path out() {
        return out;
    }

    public Path vanilla(String version) {
        return minecraft.resolve(version);
    }

    public Collection<String> versions() {
        return rpMapmakerVersionsPaths.keySet();
    }

    // Resource pack methods

    /**
     * Writes a texture and returns a reference to it.
     */
    public String writeTexture(@Nullable String type, String name, byte[] data, byte @Nullable [] mcmeta) {
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
    public String writeTexture(@Nullable String type, String name, byte[] data) {
        return writeTexture(type, name, data, null);
    }

    public String writeModel(String name, JsonObject model) throws IOException {
        name = minifyId(name);

        Path path = rpMapmakerBase.resolve("models").resolve("item").resolve(name + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new Gson().toJson(model));

        return mapmakerRefBase + "item/" + name;
    }

    public void addFontCharacter(JsonObject definition) {
        fontFile.getAsJsonArray("providers").add(definition);
    }

    public void addItemModel(String name, JsonObject model) throws IOException {
        var minName = minifyId(name);

        Path path = rpMapmakerBase.resolve("items").resolve(minName + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new Gson().toJson(model));

        var clientPath = mapmakerRefBase + minName;
        addServerSprite(new ServerSprite(name, clientPath));
    }

    public void addItemModels(String name, Map<String, JsonObject> models) throws IOException {
        var minName = minifyId(name);
        var fileName = minName + ".json";

        for (var entry : models.entrySet()) {
            var path = this.rpMapmakerVersionsPaths.get(entry.getKey()).resolve("items").resolve(fileName);
            var model = entry.getValue();
            Files.createDirectories(path.getParent());
            Files.writeString(path, new Gson().toJson(model));
        }

        var clientPath = mapmakerRefBase + minName;
        addServerSprite(new ServerSprite(name, clientPath));
    }

    public void addServerSprite(ServerSprite sprite) {
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

    private String minifyId(String id) {
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
