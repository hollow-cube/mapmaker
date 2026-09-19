package net.hollowcube.mapmaker.util;

import net.minestom.server.network.packet.server.play.MapDataPacket;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class ImageBuffer {

    public static final int SIZE = 128;

    // Vanilla map base colors (MapColor), index 0 is transparent and never matched.
    private static final int[] BASE_COLORS = {
        0x000000, 0x7FB238, 0xF7E9A3, 0xC7C7C7, 0xFF0000, 0xA0A0FF, 0xA7A7A7, 0x007C00,
        0xFFFFFF, 0xA4A8B8, 0x976D4D, 0x707070, 0x4040FF, 0x8F7748, 0xFFFCF5, 0xD87F33,
        0xB24CD8, 0x6699D8, 0xE5E533, 0x7FCC19, 0xF27FA5, 0x4C4C4C, 0x999999, 0x4C7F99,
        0x7F3FB2, 0x334CB2, 0x664C33, 0x667F33, 0x993333, 0x191919, 0xFAEE4D, 0x5CDBD5,
        0x4A80FF, 0x00D93A, 0x815631, 0x700200, 0xD1B1A1, 0x9F5224, 0x95576C, 0x706C8A,
        0xBA8524, 0x677535, 0xA04D4E, 0x392923, 0x876B62, 0x575C5C, 0x7A4958, 0x4C3E5C,
        0x4C3223, 0x4C522A, 0x8E3C2E, 0x251610, 0xBD3031, 0x943F61, 0x5C191D, 0x167E86,
        0x3A8E8C, 0x562C3E, 0x14B485,
    };
    // Shade multipliers for color ids base * 4 + 0..3, in that order.
    private static final double[] SHADES = {0.71, 0.86, 1.0, 0.53};

    private final byte[] data = new byte[SIZE * SIZE];

    public void fill(int argb) {
        Arrays.fill(this.data, colorIndex(argb));
    }

    public void setPixel(int x, int y, int argb) {
        this.data[y * SIZE + x] = colorIndex(argb);
    }

    public void draw(BufferedImage image, boolean center) {
        int width = image.getWidth();
        int height = image.getHeight();

        int u = width > SIZE ? (width - SIZE) / 2 : 0;
        int v = height > SIZE ? (height - SIZE) / 2 : 0;

        int xOff = center ? Math.max(0, (SIZE - width) / 2) : 0;
        int yOff = center ? Math.max(0, (SIZE - height) / 2) : 0;

        for (int x = 0; x < Math.min(SIZE, width); x++) {
            for (int y = 0; y < Math.min(SIZE, height); y++) {
                this.setPixel(x + xOff, y + yOff, image.getRGB(u + x, v + y));
            }
        }
    }

    public MapDataPacket preparePacket(int mapId) {
        return new MapDataPacket(mapId, (byte) 0, false, false, List.of(),
            new MapDataPacket.ColorContent((byte) SIZE, (byte) SIZE, (byte) 0, (byte) 0, this.data));
    }

    private static byte colorIndex(int argb) {
        if (((argb >> 24) & 0xFF) < 4) return 0;
        return closestColor(argb & 0xFFFFFF);
    }

    private static byte closestColor(int rgb) {
        int red = (rgb >> 16) & 0xFF, green = (rgb >> 8) & 0xFF, blue = rgb & 0xFF;
        int closest = 0, closestDistance = Integer.MAX_VALUE;
        for (int base = 1; base < BASE_COLORS.length; base++) {
            for (int shade = 0; shade < SHADES.length; shade++) {
                int dr = (int) (((BASE_COLORS[base] >> 16) & 0xFF) * SHADES[shade]) - red;
                int dg = (int) (((BASE_COLORS[base] >> 8) & 0xFF) * SHADES[shade]) - green;
                int db = (int) ((BASE_COLORS[base] & 0xFF) * SHADES[shade]) - blue;
                int distance = dr * dr + dg * dg + db * db;
                if (distance < closestDistance) {
                    closest = base * 4 + shade;
                    closestDistance = distance;
                }
            }
        }
        return (byte) closest;
    }
}
