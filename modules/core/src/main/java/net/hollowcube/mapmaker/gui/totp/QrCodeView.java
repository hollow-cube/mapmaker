package net.hollowcube.mapmaker.gui.totp;

import net.hollowcube.mapmaker.panels.AbstractImagePanel;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.minestom.server.map.Framebuffer;

import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.BitSet;

public class QrCodeView extends AbstractImagePanel {

    private static final int SIZE = Framebuffer.WIDTH;

    private final BufferedImage qrCode;
    private final Panel toOpen;

    /**
     * @param base64  The base64 encoded QR code bits, 1 is black, 0 is white, must be a square
     * @param toOpen The panel to open when the user clicks the QR code
     */
    public QrCodeView(String base64, int size, Panel toOpen) {
        super("info");
        this.qrCode = createQrCode(base64, size);
        this.toOpen = toOpen;
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        this.updateImage(buffer -> {
            buffer.fill(0x00000000);
            buffer.draw(this.qrCode, true);
        });
    }

    @Override
    protected void onSubmit() {
        this.host.pushTransientView(this.toOpen);
    }

    private static BufferedImage createQrCode(String base64Bits, int size) {
        var bits = BitSet.valueOf(Base64.getDecoder().decode(base64Bits));
        int scale = size < SIZE ? Math.floorDiv(SIZE, size) : 1;
        var image = new BufferedImage(size * scale, size * scale, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int index = (y * size) + x;
                int pixel = index < bits.length() && bits.get(index) ? 0xFF000000 : 0xFFFFFFFF;

                for (int dx = 0; dx < scale; dx++) {
                    for (int dy = 0; dy < scale; dy++) {
                        image.setRGB(x * scale + dx, y * scale + dy, pixel);
                    }
                }
            }
        }

        return image;
    }
}
