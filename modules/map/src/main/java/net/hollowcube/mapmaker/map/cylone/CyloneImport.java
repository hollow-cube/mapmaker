package net.hollowcube.mapmaker.map.cylone;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CyloneImport {

    public static void tempConvert() throws Exception {
        var json = Files.readString(Path.of("/Users/matt/Downloads/Export to Hollow Cube/cylone_v2.parkour_map_definitions.json"));
        var array = new Gson().fromJson(json, JsonArray.class);

        var base = Path.of("/Users/matt/Downloads/Export to Hollow Cube/maps");
        var out = Path.of("/Users/matt/Downloads/Export to Hollow Cube/out-polar");
        var fileList = Files.list(base).toList();
        for (var slimeWorld : fileList) {
            var name = slimeWorld.getFileName().toString().replaceAll(".slime", "").replaceAll("active_", "");

            JsonObject mapDetails = null;
            for (var element : array) {
                var obj = element.getAsJsonObject();
                if (obj.get("_id").getAsString().equals(name)) {
                    mapDetails = obj;
                    break;
                }
            }
            if (mapDetails == null) {
                throw new RuntimeException("no map details: " + name);
            }

            var data = Files.readAllBytes(slimeWorld);
            byte[] polar;
            try {
                polar = SlimeToPolar.convertSlimeToPolar(data, mapDetails);
            } catch (UnsupportedOperationException e) {
                System.out.println("v9: " + name);
                continue;
            } catch (Exception e) {
                System.out.println("other issue: " + name + " " + e.getMessage());
                continue;
            }

            Files.createDirectories(out.resolve(name + ".polar").getParent());
            Files.write(out.resolve(name + ".polar"), polar, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("done with " + name);
        }

        System.out.println("DONE WITH ALL!!!!");
        System.exit(0);
    }
}
