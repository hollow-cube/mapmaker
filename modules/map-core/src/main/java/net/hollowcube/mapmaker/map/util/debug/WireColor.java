package net.hollowcube.mapmaker.map.util.debug;

/// Colors baked by packer into the mapmaker:wire font.
///
/// Values MUST stay in-sync with resources/wire_font.json5.
public enum WireColor {
    WHITE, LIGHT_GRAY, DARK_GRAY, LIGHT_BLUE, YELLOW, RED;

    private final String lineGlyph;
    private final String boxGlyphs;

    WireColor() {
        this.lineGlyph = Character.toString(0xE000 + ordinal());
        var boxGlyphs = new StringBuilder();
        for (int edge = 0; edge < DebugDraw.Encoding.EDGES; edge++)
            boxGlyphs.appendCodePoint(0xE100 + ordinal() * 16 + edge);
        this.boxGlyphs =  boxGlyphs.toString();
    }

    String lineGlyph() {
        return this.lineGlyph;
    }

    String boxGlyphs() {
        return this.boxGlyphs;
    }
}
