package net.hollowcube.mapmaker.gui.common.cartography;

import net.minestom.server.map.Framebuffer;
import net.minestom.server.map.MapColors;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class ImageBuffer implements Framebuffer {

    private final byte[] data = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT];

    private int index(int x, int y) {
        return y * Framebuffer.WIDTH + x;
    }

    private void fill(byte index) {
        Arrays.fill(this.data, index);
    }

    public void fill(MapColors color) {
        this.fill(color.baseColor());
    }

    public void fill(int argb) {
        byte index = ((argb >> 24) & 0xFF) < 4 ? 0 : MapColors.closestColor(argb).getIndex();
        this.fill(index);
    }

    private void setPixel(int x, int y, byte index) {
        this.data[this.index(x, y)] = index;
    }

    public void setPixel(int x, int y, MapColors color) {
        this.setPixel(x, y, color.baseColor());
    }

    public void setPixel(int x, int y, int argb) {
        byte index = ((argb >> 24) & 0xFF) < 4 ? 0 : MapColors.closestColor(argb).getIndex();
        this.setPixel(x, y, index);
    }

    public void draw(BufferedImage image, boolean center) {
        int width = image.getWidth();
        int height = image.getHeight();

        int u = width > Framebuffer.WIDTH ? (width - Framebuffer.WIDTH) / 2 : 0;
        int v = height > Framebuffer.HEIGHT ? (height - Framebuffer.HEIGHT) / 2 : 0;

        int xOff = center ? Math.max(0, (Framebuffer.WIDTH - width) / 2) : 0;
        int yOff = center ? Math.max(0, (Framebuffer.HEIGHT - height) / 2) : 0;

        for (int x = 0; x < Math.min(Framebuffer.WIDTH, width); x++) {
            for (int y = 0; y < Math.min(Framebuffer.HEIGHT, height); y++) {
                int argb = image.getRGB(u + x, v + y);
                this.setPixel(x + xOff, y + yOff, argb);
            }
        }
    }

    @Override
    public byte[] toMapColors() {
        return this.data;
    }
}
