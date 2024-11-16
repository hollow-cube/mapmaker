package net.hollowcube.mapmaker.gui.world;

import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IWGTextElement extends IWGElement {
    private static final float DEFAULT_LINE_HEIGHT_BLOCKS = 0.25f;
    public static final float LINE_HEIGHT_PIXELS = 10;
    // The size of a pixel can be used to compute the size of any given string of text.
    private static final float DEFAULT_PIXEL_SIZE_BLOCKS = DEFAULT_LINE_HEIGHT_BLOCKS / LINE_HEIGHT_PIXELS;

    private List<Component> textLines = List.of(); // Split by line

    public IWGTextElement() {
    }

    public @NotNull IWGTextElement text(@NotNull Component textLines) {
        if (PlainTextComponentSerializer.plainText().serialize(textLines).contains("\n"))
            throw new IllegalArgumentException("Text must not contain newlines yet");
        this.textLines = List.of(textLines); // TODO: multiline support
        return this;
    }

    @Override
    public @NotNull Point boundingBox() {
        if (textLines.isEmpty()) return Vec.ZERO;

        int pixelWidth = 0;
        for (var line : textLines) {
            int lineWidth = FontUtil.measureTextV2(line) + 1; // +1 for first pixel
            pixelWidth = Math.max(pixelWidth, lineWidth);
        }
        if (pixelWidth <= 1) return Vec.ZERO; // No text, no size

        return new Vec(0, DEFAULT_LINE_HEIGHT_BLOCKS * textLines.size(), 0);
    }
}
