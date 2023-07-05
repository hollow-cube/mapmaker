package net.hollowcube.terraform.give_me_new_home;

import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.schem.SchematicReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TempSchematicStuff {
//    private static final String BASE_PATH = System.getenv("MM_SCHEMATICS");
    private static final String BASE_PATH = "/Users/matt/dev/projects/hollowcube/mapmaker/schem";
    private static final List<String> schematics = new ArrayList<>();


    public static @NotNull List<String> getSchematics() {
        return schematics;
    }

    public static @Nullable Schematic load(@NotNull String name) {
        try {
            var schem = SchematicReader.read(Path.of(BASE_PATH, name));
            return new Schematic(schem.size(), schem.offset(), schem.palette(), schem.blocks());
        } catch (Exception ignored) {
            return null;
        }
    }

    static {
        init();
    }

    private static void init() {
        if (BASE_PATH == null) return;
        var basePath = Path.of(BASE_PATH);
        if (Files.notExists(basePath)) return;
        if (!Files.isDirectory(basePath)) return;

        try (var stream = Files.list(basePath)) {
            stream.forEach(path -> {
                if (Files.isDirectory(path)) return;
                if (!path.getFileName().toString().endsWith(".schem")) return;
                schematics.add(path.getFileName().toString());
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
