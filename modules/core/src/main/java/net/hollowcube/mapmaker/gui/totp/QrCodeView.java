package net.hollowcube.mapmaker.gui.totp;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.common.cartography.AbstractImageView;
import net.minestom.server.map.Framebuffer;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.BitSet;
import java.util.function.Function;

public class QrCodeView extends AbstractImageView {

    private static final int SIZE = Framebuffer.WIDTH;

    private final BufferedImage qrCode;
    private final Function<Context, View> newView;

    /**
     * @param context The view context
     * @param base64  The base64 encoded QR code bits, 1 is black, 0 is white, must be a square
     * @param newView The callback to run when the user confirms the QR code
     */
    public QrCodeView(@NotNull Context context, String base64, int size, Function<Context, View> newView) {
        super(context);
        this.qrCode = createQrCode(base64, size);
        this.newView = newView;
    }

    @Signal(View.SIG_MOUNT)
    public void onMount() {
        this.updateImage(buffer -> {
            buffer.fill(0x00000000);
            buffer.draw(this.qrCode, true);
        });
    }

    @Action("confirm")
    public void onConfirm() {
        this.pushTransientView(this.newView);
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
