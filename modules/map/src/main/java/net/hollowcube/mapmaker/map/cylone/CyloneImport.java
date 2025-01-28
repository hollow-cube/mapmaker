package net.hollowcube.mapmaker.map.cylone;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.*;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class CyloneImport {
    private static final String AUTHORIZER = "aceb326f-da15-45bc-bf2f-11940c21780c";
    private static final String CYLONE_ORG_ID = "7a7ef7d2-3f3f-4a95-913e-0d3d70c0d122";

    private static final List<String> FINISHED_IDS = List.of("3x3", "Agony", "Ancient", "AquaticPrison", "Balloons", "BigBook");

    public static void tempConvert() throws Exception {
        var json = Files.readString(Path.of("/Users/matt/Downloads/Export to Hollow Cube/cylone_v2.parkour_map_definitions.json"));
        var array = new Gson().fromJson(json, JsonArray.class);
        FutureUtil.markShutdown();

        var mapService = new MapServiceImpl("http://localhost:9125");
        var existingMaps = getExistingMaps(mapService);

        var base = Path.of("/Users/matt/Downloads/Export to Hollow Cube/maps");
        var out = Path.of("/Users/matt/Downloads/Export to Hollow Cube/out-polar");
        var fileList = Files.list(base).toList();
        for (var slimeWorld : fileList) {
            var name = slimeWorld.getFileName().toString().replaceAll(".slime", "").replaceAll("active_", "");
            if (FINISHED_IDS.contains(name)) continue;
            if (!"Multiverse".equals(name)) continue;

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
            var mapNameComponent = GsonComponentSerializer.gson().deserialize(mapDetails.get("displayName").getAsString());
            var mapNameString = PlainTextComponentSerializer.plainText().serialize(mapNameComponent);

            Pos spawnPoint = null;
            var locationsObject = mapDetails.getAsJsonObject("locations");
            if (locationsObject.has("SINGLE_INSTANCE_START")) {
                var start = locationsObject.getAsJsonObject("SINGLE_INSTANCE_START");
                spawnPoint = new Pos(
                        start.get("x").getAsDouble(),
                        start.get("y").getAsDouble(),
                        start.get("z").getAsDouble(),
                        start.get("yaw").getAsFloat(),
                        start.get("pitch").getAsFloat()
                );
            }

            MapData tMap = existingMaps.stream()
                    .filter(m -> m.name().equals(mapNameString))
                    .findFirst()
                    .orElse(null);
            if (tMap == null) {
                tMap = mapService.createOrgMap(AUTHORIZER, CYLONE_ORG_ID);
                existingMaps.add(tMap);
            }
            var map = tMap;
            map.setSetting(MapSettings.RESET_IN_LAVA, true);
            map.settings().setName(mapNameString);
            map.settings().setVariant(MapVariant.PARKOUR);
            map.settings().setIcon(Material.GREEN_CONCRETE);
            if (spawnPoint != null) map.settings().setSpawnPoint(spawnPoint);

            var data = Files.readAllBytes(slimeWorld);
            byte[] polar;
            try {
                polar = SlimeToPolar.convertSlimeToPolar(data, mapDetails);
            } catch (UnsupportedOperationException e) {
                System.out.println("v9: " + name);
                continue;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            Files.createDirectories(out.resolve(name + ".polar").getParent());
            Files.write(out.resolve(name + ".polar"), polar, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            var out2 = out.getParent().resolve("hcout").resolve(map.id());
            Files.createDirectories(out2.getParent());
            Files.write(out2, polar, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            map.settings().withUpdateRequest(req -> {
                mapService.updateMap(AUTHORIZER, map.id(), req);
                return true;
            });
            mapService.updateMapWorld(map.id(), polar);
        }

        try (var writer = Files.newBufferedWriter(out.getParent().resolve("mapping.csv"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("id,name\n");
            existingMaps.sort((a, b) -> a.name().compareTo(b.name()));
            for (var map : existingMaps) {
                writer.write(map.id() + "," + map.name() + "\n");
            }
        }

        System.out.println("DONE WITH ALL!!!!");
        System.exit(0);
    }

    private static List<MapData> getExistingMaps(@NotNull MapService mapService) {
        var results = new ArrayList<MapData>();
        int lastPage = 0;
        while (true) {
            var result = mapService.searchOrgMaps(AUTHORIZER, lastPage, 50, CYLONE_ORG_ID);
            if (!result.nextPage()) break;
            lastPage++;
            results.addAll(result.results());
        }
        return results;
    }
}
