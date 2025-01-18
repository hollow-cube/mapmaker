package net.hollowcube.mapmaker.dev;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.map.cylone.SlimeToPolar;
import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class DevServer {

    public static void main(String[] args) throws Exception {

        var json = new Gson().fromJson("""
                {
                  "_id": "Jungle",
                  "authors": [
                    "{\\"color\\":\\"aqua\\",\\"text\\":\\"Zombie1111\\"}"
                  ],
                  "checkpoints": [
                    {
                      "x": 3,
                      "y": 26,
                      "z": 16
                    },
                    {
                      "x": 4,
                      "y": 28,
                      "z": -4
                    },
                    {
                      "x": -2,
                      "y": 32,
                      "z": -25
                    },
                    {
                      "x": -21,
                      "y": 34,
                      "z": -25
                    },
                    {
                      "x": -22,
                      "y": 39,
                      "z": -3
                    },
                    {
                      "x": -23,
                      "y": 42,
                      "z": 23
                    },
                    {
                      "x": -22,
                      "y": 46,
                      "z": 38
                    },
                    {
                      "x": 2,
                      "y": 48,
                      "z": 43
                    },
                    {
                      "x": -2,
                      "y": 53,
                      "z": 22
                    },
                    {
                      "x": -2,
                      "y": 50,
                      "z": -1
                    },
                    {
                      "x": 3,
                      "y": 55,
                      "z": -22
                    },
                    {
                      "x": -3,
                      "y": 60,
                      "z": -32
                    },
                    {
                      "x": -14,
                      "y": 67,
                      "z": -42
                    },
                    {
                      "x": -20,
                      "y": 68,
                      "z": -20
                    },
                    {
                      "x": -17,
                      "y": 70,
                      "z": -2
                    },
                    {
                      "x": -21,
                      "y": 72,
                      "z": 19
                    },
                    {
                      "x": -20,
                      "y": 71,
                      "z": 46
                    },
                    {
                      "x": -2,
                      "y": 73,
                      "z": 41
                    },
                    {
                      "x": 0,
                      "y": 75,
                      "z": 25
                    },
                    {
                      "x": -1,
                      "y": 79,
                      "z": 10
                    },
                    {
                      "x": 0,
                      "y": 83,
                      "z": 2
                    },
                    {
                      "x": 0,
                      "y": 83,
                      "z": -25
                    },
                    {
                      "x": -8,
                      "y": 86,
                      "z": -39
                    },
                    {
                      "x": -19,
                      "y": 88,
                      "z": -18
                    },
                    {
                      "x": -18,
                      "y": 88,
                      "z": 2
                    },
                    {
                      "x": -17,
                      "y": 90,
                      "z": 20
                    },
                    {
                      "x": -18,
                      "y": 92,
                      "z": 36
                    },
                    {
                      "x": -29,
                      "y": 89,
                      "z": -14
                    },
                    {
                      "x": -42,
                      "y": 94,
                      "z": -17
                    },
                    {
                      "x": -34,
                      "y": 88,
                      "z": -36
                    },
                    {
                      "x": -10,
                      "y": 88,
                      "z": -56
                    },
                    {
                      "x": -2,
                      "y": 97,
                      "z": -55
                    },
                    {
                      "x": 20,
                      "y": 93,
                      "z": -35
                    },
                    {
                      "x": 20,
                      "y": 89,
                      "z": -17
                    },
                    {
                      "x": 20,
                      "y": 94,
                      "z": 2
                    },
                    {
                      "x": 13,
                      "y": 86,
                      "z": 31
                    },
                    {
                      "x": 19,
                      "y": 90,
                      "z": 45
                    },
                    {
                      "x": 6,
                      "y": 96,
                      "z": 46
                    },
                    {
                      "x": -20,
                      "y": 88,
                      "z": 67
                    },
                    {
                      "x": -41,
                      "y": 87,
                      "z": 39
                    },
                    {
                      "x": -31,
                      "y": 95,
                      "z": 26
                    },
                    {
                      "x": -29,
                      "y": 89,
                      "z": -1
                    }
                  ],
                  "description": [],
                  "difficulty": "EASY",
                  "displayName": "{\\"bold\\":true,\\"color\\":\\"green\\",\\"text\\":\\"Jungle\\"}",
                  "effectGivers": {},
                  "environment": "NORMAL",
                  "itemGivers": {},
                  "locations": {
                    "MAP_LEADERBOARD_HOLOGRAM_LOCATION": {
                      "x": 0.5,
                      "y": 100.5,
                      "z": 6.5,
                      "yaw": 3.1811201572418213,
                      "pitch": 84.39337158203125
                    },
                    "MAP_LOBBY": {
                      "x": 0.5,
                      "y": 101,
                      "z": 0.5,
                      "yaw": -270.04931640625,
                      "pitch": -0.0015258184866979718
                    },
                    "SINGLE_INSTANCE_START": {
                      "x": -3.5,
                      "y": 24,
                      "z": 10.5,
                      "yaw": -0.007902086712419987,
                      "pitch": 0.15568575263023376
                    }
                  },
                  "mapType": "SINGLE_INSTANCE",
                  "selections": {}
                }
                """, JsonObject.class);
        var data = Files.readAllBytes(Path.of("/Users/matt/Downloads/active_Jungle-2.slime"));
        var polar = SlimeToPolar.convertSlimeToPolar(data, json);

        Files.write(Path.of("/Users/matt/Downloads/98e7c61f-9fea-484a-b370-1916791df6ab"), polar, StandardOpenOption.TRUNCATE_EXISTING);

        MapServerInitializer.run(DevServerRunner::new, args);

//        var path = Path.of("/Users/matt/dev/projects/hollowcube/mapmaker/modules/hub/src/main/resources/spawn/hcspawn.polar");
//        var world = PolarReader.read(Files.readAllBytes(path));
//
//        var newWorld = new PolarWorld(
//                world.version(),
//                world.dataVersion(),
//                world.compression(),
//                world.minSection(), world.maxSection(),
//                world.userData(),
//                new ArrayList<>()
//        );
//
//        int total = 0;
//        outer:
//        for (var chunk : world.chunks()) {
//            for (var section : chunk.sections()) {
//                boolean isEmpty = section.isEmpty() || (section.blockPalette().length == 1 && NamespaceID.from(section.blockPalette()[0]).asMinimalString().equals("air"));
//                if (!isEmpty) {
//                    newWorld.updateChunkAt(chunk.x(), chunk.z(), chunk);
//                    total++;
//                    continue outer;
//                }
//            }
//
//            System.out.println("EMPTY SECTION AT " + chunk.x() + ", " + chunk.z());
//
//
//        }
//        System.out.println(total);
//
//        var newBytes = PolarWriter.write(newWorld);
//        System.out.println("total size: " + newBytes.length + " old is " + Files.readAllBytes(path).length);
//        Files.write(path, newBytes);
    }
}
