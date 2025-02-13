package net.hollowcube.mapmaker.dev.element;

public enum Layer {
    /**
     * The background layer does not affect layout of a GUI (but is still affected by it).
     * Useful for background textures or text.
     */
    BACKGROUND,
    /**
     * The default location for all elements. Element sizes affect layout.
     */
    DEFAULT,
    /**
     * The item layer is rendered using physical items in the GUI.
     * Mostly useful because items have slot highlighting on hover.
     *
     * <p>Sprites used in this layer must be generated as an item.</p>
     */
    ITEM
}
