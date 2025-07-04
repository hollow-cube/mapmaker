package net.hollowcube.aj;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hollowcube.multipart.bedrock.BedrockGeoModel;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.registry.RegistryTranscoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class ResourcePackGen {
    public static void main(String[] args) throws Exception {
        var server = MinecraftServer.init();

        genForBedrock();

        System.exit(0);
    }

    private static boolean isValidRotation(Vec rotation) {
        if (rotation == null || rotation.isZero()) return true;

        // if we have multiple axes, its invalid
        if (rotation.x() != 0 && rotation.y() != 0) return false;
        if (rotation.x() != 0 && rotation.z() != 0) return false;
        if (rotation.y() != 0 && rotation.z() != 0) return false;

        // all rotations must be -45, -22.5, 0, 22.5, or 45
        if (rotation.x() != 0 && rotation.x() != -45 && rotation.x() != -22.5 && rotation.x() != 22.5 && rotation.x() != 45)
            return false;
        if (rotation.y() != 0 && rotation.y() != -45 && rotation.y() != -22.5 && rotation.y() != 22.5 && rotation.y() != 45)
            return false;
        if (rotation.z() != 0 && rotation.z() != -45 && rotation.z() != -22.5 && rotation.z() != 22.5 && rotation.z() != 45)
            return false;

        return true;
    }

    public static BedrockGeoModel fixRotation(BedrockGeoModel model) {
        var random = new Random(12345);
        var newBones = new ArrayList<BedrockGeoModel.Bone>();
        for (var bone : model.bones()) {
            var newCubes = new ArrayList<BedrockGeoModel.Cube>();
            for (var cube : bone.cubes()) {

                if (!isValidRotation(cube.rotation())) {
                    // We need to make a new bone to handle this case.
                    var newBoneCubes = new ArrayList<BedrockGeoModel.Cube>();
                    //todo should the cube pivot be zeroed?
                    newBoneCubes.add(new BedrockGeoModel.Cube(cube.origin(), cube.size(), null, null, cube.inflate(), cube.uv()));

                    var newBone = new BedrockGeoModel.Bone(bone.name() + "_rotated_" + random.nextInt(999999), bone.name(), cube.pivot(), cube.rotation(), newBoneCubes);
                    newBones.add(newBone);
                    continue; // Skip we have handled this separately
                }

                newCubes.add(new BedrockGeoModel.Cube(cube.origin(), cube.size(), cube.pivot(), cube.rotation(), cube.inflate(), cube.uv()));
            }

            newBones.add(new BedrockGeoModel.Bone(bone.name(), bone.parent(), bone.pivot(), bone.rotation(), newCubes));
        }
        return new BedrockGeoModel(model.description(), newBones);
    }

    private static void genForBedrock() throws Exception {
        var namespace = "mymap";
        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/models/entity/assembler.geo.json");
        var texturePath = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/textures/entity/assembler.png");
        var json = new Gson().fromJson(Files.readString(path), JsonObject.class).getAsJsonArray("minecraft:geometry").get(0);
        var model = BedrockGeoModel.CODEC.decode(new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process()), json).orElseThrow();
        model = fixRotation(model);

        var packOut = Path.of("/Users/matt/Library/Application Support/mc-cli/profiles/1.21.6-fabric/resourcepacks/aj-generated");
        Files.createDirectories(packOut);

        Files.writeString(packOut.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "pack_format": 31,
                    "description": "AJ Generated Pack"
                  }
                }
                """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        var assets = packOut.resolve("assets").resolve(namespace);
        Files.createDirectories(assets);

        // Write textures
        var texPath = assets.resolve("textures/item").resolve("assembler.png");
        Files.createDirectories(texPath.getParent());
        Files.write(texPath, Files.readAllBytes(texturePath), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Write models
        for (var variantModel : model.bones()) {
            if (variantModel.cubes().isEmpty()) continue;
            var modelPath = assets.resolve("models/item").resolve(variantModel.name() + ".json");
            Files.createDirectories(modelPath.getParent());

            var modelJson = new JsonObject();
            var textures = new JsonObject();
            textures.addProperty("0", namespace + ":item/" + "assembler");
            textures.addProperty("particle", namespace + ":item/" + "assembler");
            modelJson.add("textures", textures);

            var elements = new JsonArray();
            modelJson.add("elements", elements);
            for (var cube : variantModel.cubes()) {
                var element = new JsonObject();

                var from = new JsonArray();
                from.add(16 - (cube.origin().x() + cube.size().x() + cube.inflate() - variantModel.pivot().x()) / 4);
                from.add((cube.origin().y() - cube.inflate() - variantModel.pivot().y()) / 4);
                from.add((cube.origin().z() - cube.inflate() - variantModel.pivot().z()) / 4);
                element.add("from", from);
                var to = new JsonArray();
                to.add(16 - (cube.origin().x() - cube.inflate() - variantModel.pivot().x()) / 4);
                to.add((cube.origin().y() + cube.size().y() + cube.inflate() - variantModel.pivot().y()) / 4);
                to.add((cube.origin().z() + cube.size().z() + cube.inflate() - variantModel.pivot().z()) / 4);
                element.add("to", to);

                var faces = new JsonObject();
                for (var face : cube.uv().entrySet()) {
                    var f = new JsonObject();
                    var uv = new JsonArray();

                    if (face.getKey().equals("up") || face.getKey().equals("down")) {
                        // Flip up/down UVs because bedrock
                        uv.add((face.getValue().uv()[0] + face.getValue().uvSize()[0]) / 8);
                        uv.add((face.getValue().uv()[1] + face.getValue().uvSize()[1]) / 8);
                        uv.add(face.getValue().uv()[0] / 8);
                        uv.add((face.getValue().uv()[1]) / 8);
                    } else {
                        uv.add(face.getValue().uv()[0] / 8);
                        uv.add(face.getValue().uv()[1] / 8);
                        uv.add((face.getValue().uv()[0] + face.getValue().uvSize()[0]) / 8);
                        uv.add((face.getValue().uv()[1] + face.getValue().uvSize()[1]) / 8);
                    }
                    f.add("uv", uv);
                    f.addProperty("texture", "#0");
                    faces.add(face.getKey(), f);
                }
                element.add("faces", faces);

//                if (cube.rotation() != null && !cube.rotation().isZero()) {
//                    var rot = new JsonObject();
//                    rot.addProperty("angle", cube.rotation().z());
//                    rot.addProperty("axis", "z");
//                    var origin = new JsonArray();
//                    origin.add((cube.pivot().x()) / 4);
//                    origin.add((cube.pivot().y()) / 4);
//                    origin.add((cube.pivot().z()) / 4);
//                    rot.add("origin", origin);
//                    element.add("rotation", rot);
//                }

                elements.add(element);
            }

            var display = new JsonObject();
            var head = new JsonObject();
            var scale = new JsonArray();
            scale.add(4);
            scale.add(4);
            scale.add(4);
            head.add("scale", scale);
            var translation = new JsonArray();
            // The +8 offset is because blockbench centers are offset by 8 units from the head item model origin
            // *4 because thats our scaling factor
            translation.add(-8 * 4);
            translation.add(8 * 4);
            translation.add(8 * 4);
            head.add("translation", translation);
//            var rotation = new JsonArray();
//            rotation.add(0);
//            rotation.add(180);
//            rotation.add(0);
//            head.add("rotation", rotation);
            display.add("head", head);
            modelJson.add("display", display);

            System.out.println(modelPath);
            Files.writeString(modelPath, modelJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        // Write item models
        for (var variantModel : model.bones()) {
            var modelPath = assets.resolve("items").resolve(variantModel.name() + ".json");
            Files.createDirectories(modelPath.getParent());
            Files.writeString(modelPath, """
                            {
                              "model": {
                                "type": "minecraft:model",
                                "model": "%s"
                              }
                            }
                            """.formatted(namespace + ":item/" + variantModel.name()),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private static void genForAnimatedJava() throws Exception {
        var namespace = "mymap";
        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/ajmodel/buggy_tier_1.json");
//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/ajmodel/assembler.json");
//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/ajmodel/bass_ribbit.json");
//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/script-bundle/sketching/mymap/ajmodel/armor_stand.json");
        var json = new Gson().fromJson(Files.readString(path), JsonElement.class);
        var model = Model.CODEC.decode(new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process()), json).orElseThrow();

        var packOut = Path.of("/Users/matt/Library/Application Support/mc-cli/profiles/1.21.5-fabric/resourcepacks/aj-generated");
        Files.createDirectories(packOut);

        Files.writeString(packOut.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "pack_format": 31,
                    "description": "AJ Generated Pack"
                  }
                }
                """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        var assets = packOut.resolve("assets").resolve(namespace);
        Files.createDirectories(assets);

        // Write textures
        Map<String, String> textureIdMap = new HashMap<>();
        for (var texture : model.textures().entrySet()) {
            var texPath = assets.resolve("textures/item").resolve(texture.getKey() + ".png");
            Files.createDirectories(texPath.getParent());
            Files.write(texPath, Base64.getDecoder().decode(texture.getValue().src()
                    .substring("data:image/png;base64,".length())), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            var ajId = "animated_java:blueprint/" + model.settings().exportNamespace() + "/" + texture.getValue().name().replace(".png", "");
            textureIdMap.put(ajId, namespace + ":item/" + texture.getKey());
        }

        // Write models
        var variant = model.variants().values().stream().filter(Variant::isDefault).findFirst().orElseThrow();
        for (var variantModel : variant.models().entrySet()) {
            var modelPath = assets.resolve("models/item").resolve(variantModel.getKey() + ".json");
            Files.createDirectories(modelPath.getParent());
            var modelJson = variantModel.getValue().model().convertTo(Transcoder.JSON).orElseThrow().getAsJsonObject();
            var textures = modelJson.getAsJsonObject("textures");
            for (var key : Set.copyOf(textures.keySet())) {
                var val = textures.get(key).getAsString();
                if (textureIdMap.containsKey(val)) {
                    textures.addProperty(key, textureIdMap.get(val));
                } else {
                    System.err.println("Warning: No mapping for texture id " + val);
                }
            }

            Files.writeString(modelPath, modelJson.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        // Write item models
        for (var variantModel : variant.models().entrySet()) {
            var modelPath = assets.resolve("items").resolve(variantModel.getKey() + ".json");
            Files.createDirectories(modelPath.getParent());
            Files.writeString(modelPath, """
                            {
                              "model": {
                                "type": "minecraft:model",
                                "model": "%s"
                              }
                            }
                            """.formatted(namespace + ":item/" + variantModel.getKey()),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
