package net.hollowcube.mapmaker;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Element;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// Generates the mapmaker:wire font from `wire_font.json5`, one wire glyph and twelve box edge glyphs per style.
///
/// The text shader finds these glyphs by the marker colour in their corners and reads the pixels beside each
/// corner as data: the style's tint, then for box glyphs the edge index in red.
public class WireFontTransform {
    private static final Json5 json5 = new Json5();

    private static final int SIZE = 8;
    private static final int EDGES = 12;
    private static final int WIRE_ID = 0xFE4E2C;
    private static final int BOX_ID = 0xFE4F2C;
    private static final int WIRE_CHAR = 0xE000;
    private static final int BOX_CHAR = 0xE100;

    public void process(@NotNull PackContext ctx) throws IOException {
        generate(ctx.resources().resolve("wire_font.json5"), ctx.out().resolve("client"));
    }

    static void generate(@NotNull Path definition, @NotNull Path clientDirectory) throws IOException {
        var styles = json5.parse(Files.readString(definition)).getAsJson5Object().getAsJson5Array("styles");

        Path assets = clientDirectory.resolve("assets").resolve("mapmaker");
        Path textures = assets.resolve("textures").resolve("font");
        Files.createDirectories(textures);

        var providers = new JsonArray();
        int index = 0;
        for (Json5Element element : styles) {
            var style = element.getAsJson5Object();
            var name = style.get("name").getAsString();
            int tint = Integer.parseInt(style.get("color").getAsString().substring(1), 16);

            var wire = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            drawGlyph(wire, 0, WIRE_ID, tint, -1);
            ImageIO.write(wire, "png", textures.resolve("wire_" + name + ".png").toFile());
            providers.add(provider("wire_" + name, Character.toString(WIRE_CHAR + index)));

            var box = new BufferedImage(SIZE * EDGES, SIZE, BufferedImage.TYPE_INT_ARGB);
            var chars = new StringBuilder();
            for (int edge = 0; edge < EDGES; edge++) {
                drawGlyph(box, edge * SIZE, BOX_ID, tint, edge << 16);
                chars.appendCodePoint(BOX_CHAR + index * 16 + edge);
            }
            ImageIO.write(box, "png", textures.resolve("box_" + name + ".png").toFile());
            providers.add(provider("box_" + name, chars.toString()));

            index++;
        }

        var font = new JsonObject();
        font.add("providers", providers);
        Path fontFile = assets.resolve("font").resolve("wire.json");
        Files.createDirectories(fontFile.getParent());
        Files.writeString(fontFile, new GsonBuilder().disableHtmlEscaping().create().toJson(font));
    }

    /// Fills a glyph with the tint, then writes the marker and data pixels inward from each corner of its top and
    /// bottom rows, so every vertex finds them whichever corner it is.
    private static void drawGlyph(BufferedImage image, int x0, int marker, int tint, int edgeData) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) image.setRGB(x0 + x, y, opaque(tint));
        }
        int[] row = edgeData < 0 ? new int[]{marker, tint} : new int[]{marker, tint, edgeData};
        for (int y : new int[]{0, SIZE - 1}) {
            for (int i = 0; i < row.length; i++) {
                image.setRGB(x0 + i, y, opaque(row[i]));
                image.setRGB(x0 + SIZE - 1 - i, y, opaque(row[i]));
            }
        }
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    private static JsonObject provider(String texture, String chars) {
        var provider = new JsonObject();
        provider.addProperty("type", "bitmap");
        provider.addProperty("file", "mapmaker:font/" + texture + ".png");
        provider.addProperty("height", SIZE);
        provider.addProperty("ascent", SIZE - 1);
        var charList = new JsonArray();
        charList.add(chars);
        provider.add("chars", charList);
        return provider;
    }
}
