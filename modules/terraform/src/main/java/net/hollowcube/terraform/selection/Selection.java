package net.hollowcube.terraform.selection;

import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.selection.region.RegionSelector;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
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
        if (selector.selectPrimary(point, explain)) {
            cachedRegion = null;
            return true;
        }
        return false;
    }

    public boolean selectSecondary(@NotNull Point point, boolean explain) {
        if (selector.selectSecondary(point, explain)) {
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

    @Deprecated //todo delete me or change the mechanism
    public void changeSize(int delta, boolean changeVertical, boolean changeHorizontal) {
//        selector.changeSize(delta, changeVertical, changeHorizontal);
    }

    // Serialization

    @ApiStatus.Internal
    public void write(@NotNull NetworkBuffer buffer) {
        buffer.write(STRING, name);
        buffer.writeEnum(Region.Type.class, regionType);
        selector.write(buffer);
    }
}
