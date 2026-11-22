package net.hollowcube.mapmaker.gui.totp;

import net.hollowcube.mapmaker.panels.AbstractImagePanel;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.util.ImageBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.BitSet;

public class QrCodeView extends AbstractImagePanel {

    private static final int SIZE = ImageBuffer.SIZE;

    private final BitSet bits;
    private final int size;
    private final Panel toOpen;

    /**
     * @param base64  The base64 encoded QR code bits, 1 is black, 0 is white, must be a square
     * @param toOpen The panel to open when the user clicks the QR code
     */
    public QrCodeView(String base64, int size, Panel toOpen) {
        super("info");
        this.bits = BitSet.valueOf(Base64.getDecoder().decode(base64));
        this.size = size;
        this.toOpen = toOpen;
    }

    @Override
    protected void mount(@NotNull InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        this.updateImage(buffer -> {
            buffer.fill(0x00000000);
            drawQrCode(buffer);
        });
    }

    @Override
    protected void onSubmit() {
        this.host.pushTransientView(this.toOpen);
    }

    private void drawQrCode(ImageBuffer buffer) {
        int scale = size < SIZE ? Math.floorDiv(SIZE, size) : 1;
        int drawn = Math.min(SIZE, size * scale);
        int offset = (SIZE - drawn) / 2;
        int crop = (size * scale - drawn) / 2;

        for (int px = 0; px < drawn; px++) {
            for (int py = 0; py < drawn; py++) {
                int index = ((py + crop) / scale) * size + ((px + crop) / scale);
                int pixel = bits.get(index) ? 0xFF000000 : 0xFFFFFFFF;
                buffer.setPixel(px + offset, py + offset, pixel);
            }
        }
    }
}
