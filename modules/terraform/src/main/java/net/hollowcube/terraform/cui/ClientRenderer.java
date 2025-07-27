package net.hollowcube.terraform.cui;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

//todo should use a player configured renderer or choose the best default if they have not chosen
// eg if they have never chosen a renderer (including on every new join)
//    choose in the following order: DebugRenderer, WorldEditCUI, Particles, Nothing
//    if they have chosen a renderer, use that always.
//    if they have selected a lower priority renderer and join with a higher priority, send them a message ONCE
//    if they have selected a renderer (such as debug renderer) and join without it, leave their selection and
//    send a message indicating that they have fallen back to <next highest priority>
public interface ClientRenderer {

    /**
     * Returns a ClientRenderer which explicitly never sends anything to a client.
     *
     * @return a ClientRenderer instance that does nothing
     */
    static @NotNull ClientRenderer noop() {
        return NoopClientRenderer.INSTANCE;
    }

    /**
     * Can be used to check if a feature is available, and use a different rendering technique if not.
     *
     * @param feature The feature to check
     * @return True if the feature is supported, false otherwise.
     * @apiNote Even if a feature is not supported, it is still completely valid to call relevant methods.
     * The renderer implementation should do its best to choose a similar primary or ignore the
     * call entirely, but never return an error. For example, WorldEdit CUI does not support curves,
     * so it might fall back to the particle implementation of Bézier curves.
     */
    boolean hasFeature(@NotNull Feature feature);

    void begin(@NotNull String id);

    void end(@NotNull String id);

    /**
     * Renders a cuboid region between the given points.
     * <p>
     * Requires {@link Feature#CUBE} support.
     */
    void cuboid(@NotNull Point point1, @NotNull Point point2, @NotNull ClientRenderer.RenderType type);

    default void point(@NotNull Point point) {
        point(point, 0.05);
    }

    /**
     * Renders a single pos at the given {@link Point}.
     * <p>
     * Requires {@link Feature#POINT} support.
     */
    void point(@NotNull Point point, double radius);

    void bezierCurve(@NotNull Point p1, @NotNull Point p2, @NotNull Point p3, @NotNull Point p4);

//    /**
//     * Renders a chain of bezier curves between the given points.
//     * <p>
//     * Requires {@link Feature#POINT} support.
//     */
//    void bezierChain(@NotNull List<Point> points);

    void lineChain(@NotNull List<Point> points);

    void line(@NotNull Point p1, @NotNull Point p2, RenderType primary);

    void clearAll();

    void remove(String id);

    void switchTo(@NotNull RenderContext context, boolean store);

    @NotNull RenderContext getContext();

    void pyramid(@NotNull Point center, int height, @NotNull RenderType renderType);

    enum Feature {
        CUBE,
        POINT,
        BEZIER,
    }

    enum RenderType implements Function<RenderColors, RGBLike> {
        PRIMARY(RenderColors::primary),
        SECONDARY(RenderColors::secondary);

        private final Function<RenderColors, RGBLike> extractor;

        RenderType(Function<RenderColors, RGBLike> extractor) {
            this.extractor = extractor;
        }

        @Override
        public RGBLike apply(RenderColors context) {
            return extractor.apply(context);
        }
    }

    enum RenderContext {
        COMMAND(RenderColors.DEFAULT.withPrimary(NamedTextColor.GOLD).withSecondary(NamedTextColor.DARK_PURPLE)),
        NORMAL(RenderColors.DEFAULT);

        private final RenderColors colors;

        RenderContext(RenderColors colors) {
            this.colors = colors;
        }

        public RenderColors getColors() {
            return colors;
        }
    }

    record RenderColors(
            @NotNull RGBLike x,
            @NotNull RGBLike y,
            @NotNull RGBLike z,
            @NotNull RGBLike primary,
            @NotNull RGBLike secondary
    ) {
        public static RenderColors DEFAULT = new RenderColors(
                NamedTextColor.RED,
                NamedTextColor.GREEN,
                NamedTextColor.BLUE,
                NamedTextColor.WHITE,
                NamedTextColor.YELLOW
        );

        public RenderColors withX(RGBLike x) {
            return new RenderColors(x, this.y, this.z, this.primary, this.secondary);
        }

        public RenderColors withY(RGBLike y) {
            return new RenderColors(this.x, y, this.z, this.primary, this.secondary);
        }

        public RenderColors withZ(RGBLike z) {
            return new RenderColors(this.x, this.y, z, this.primary, this.secondary);
        }

        public RenderColors withPrimary(RGBLike primary) {
            return new RenderColors(this.x, this.y, this.z, primary, this.secondary);
        }

        public RenderColors withSecondary(RGBLike secondary) {
            return new RenderColors(this.x, this.y, this.z, this.primary, secondary);
        }
    }

}
