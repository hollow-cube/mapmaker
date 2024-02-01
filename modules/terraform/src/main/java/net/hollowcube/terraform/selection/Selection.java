package net.hollowcube.terraform.selection;

import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.MathUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minestom.server.network.NetworkBuffer.STRING;

@SuppressWarnings("UnstableApiUsage")
public final class Selection {
    public static final @NotNull String DEFAULT = "default";

    private final LocalSession session;
    private final String name;

    private RegionSelector selector;
    private Region.Type regionType;
    private Region cachedRegion = null; // Never serialized

    public Selection(@NotNull LocalSession session, @NotNull String name) {
        this(session, name, Region.Type.CUBOID);
    }

    public Selection(@NotNull LocalSession session, @NotNull NetworkBuffer buffer) {
        this(session, buffer.read(STRING), buffer.readEnum(Region.Type.class));

        selector.read(buffer);
    }

    private Selection(@NotNull LocalSession session, @NotNull String name, @NotNull Region.Type regionType) {
        this.session = session;
        this.name = name;

        this.regionType = regionType;
        this.selector = regionType.newSelector(session.cui(), name);
    }

    public String name() {
        return name;
    }

    public @NotNull Region.Type type() {
        return this.regionType;
    }

    public void setType(@NotNull Region.Type type) {
        this.regionType = type;
        this.selector = type.newSelector(session.cui(), name);
        this.selector.clear(); // Updates CUI
        this.cachedRegion = null;
    }

    @ApiStatus.Internal
    public @NotNull RegionSelector selector() {
        return selector;
    }

    public boolean selectPrimary(@NotNull Point point, boolean explain) {
        // Clamp the position to the world (border and height) of our instance.
        var clamped = clampPointToWorld(point);
        //if (explain && !point.equals(clamped)) {
        //session.cui().sendMessage("terraform.warn.border_exceeded"); //TODO I think I misinterpreted what this message was supposed to be. I still don't know what it is for tbh lol
        //}

        if (selector.selectPrimary(clamped, explain)) {
            cachedRegion = null;
            return true;
        }
        return false;
    }

    public boolean selectSecondary(@NotNull Point point, boolean explain) {
        // Clamp the position to the world (border and height) of our instance.
        var clamped = clampPointToWorld(point);
        //if (explain && !point.equals(clamped)) {
        //session.cui().sendMessage("terraform.warn.border_exceeded"); //TODO same as above
        //}

        if (selector.selectSecondary(clamped, explain)) {
            cachedRegion = null;
            return true;
        }
        return false;
    }

    public void clear() {
        selector.clear();
        cachedRegion = null;
    }

    public @Nullable Region region() {
        if (cachedRegion == null) {
            cachedRegion = selector.region();
        }
        return cachedRegion;
    }

    public void reshape(@NotNull Point low, @NotNull Point high) {
        selector.reshape(low, high);
        cachedRegion = null;
    }

    // Serialization

    @ApiStatus.Internal
    public void write(@NotNull NetworkBuffer buffer) {
        buffer.write(STRING, name);
        buffer.writeEnum(Region.Type.class, regionType);
        selector.write(buffer);
    }

    private @NotNull Point clampPointToWorld(@NotNull Point point) {
        var instance = session.instance();
        var border = instance.getWorldBorder();

        var radius = border.getDiameter() / 2;
        return new Vec(
                MathUtils.clamp(point.x(), border.getCenterX() - radius, border.getCenterX() + radius - 1),
                MathUtils.clamp(point.y(), instance.getDimensionType().getMinY(), instance.getDimensionType().getMaxY() - 1),
                MathUtils.clamp(point.z(), border.getCenterZ() - radius, border.getCenterZ() + radius - 1)
        );
    }
}
