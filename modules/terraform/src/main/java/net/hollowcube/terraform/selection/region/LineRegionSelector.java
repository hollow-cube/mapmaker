package net.hollowcube.terraform.selection.region;

import net.hollowcube.terraform.selection.cui.SelectionRenderer;
import net.hollowcube.terraform.util.CoordinateUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

public class LineRegionSelector implements RegionSelector {
    private final Player player;
    private final SelectionRenderer renderer;
    private Point pos1 = null, pos2 = null;

    public LineRegionSelector(@NotNull Player player, @NotNull SelectionRenderer renderer) {
        this.player = player;
        this.renderer = renderer;
    }

    @Override
    public boolean selectPrimary(@NotNull Point point, boolean explain) {
        if (pos1 != null && point.sameBlock(pos1)) return false;
        pos1 = CoordinateUtil.floor(point);

        updateRender();
        if (explain) {
            player.sendMessage(Component.translatable("command.worldedit.line.explain.primary",
                    Component.text(point.blockX()), Component.text(point.blockY()), Component.text(point.blockZ())));
        }

        return true;
    }

    @Override
    public boolean selectSecondary(@NotNull Point point, boolean explain) {
        if (pos2 != null && point.sameBlock(pos2)) return false;
        pos2 = CoordinateUtil.floor(point);

        updateRender();
        if (explain) {
            player.sendMessage(Component.translatable("command.worldedit.line.explain.secondary",
                    Component.text(point.blockX()), Component.text(point.blockY()), Component.text(point.blockZ())));
        }

        return true;
    }

    @Override
    public void clear() {
        pos1 = null;
        pos2 = null;
        updateRender();
    }

    @Override
    public @Nullable Region region() {
        if (pos1 == null || pos2 == null) return null;
        return new LineRegion(pos1, pos2);
    }

    private void updateRender() {
        renderer.begin();
        if (pos1 != null) renderer.point(pos1.add(0.5), 0.55);
        if (pos2 != null) renderer.point(pos2.add(0.5), 0.55);
        if (pos1 != null && pos2 != null) {
            renderer.line(pos1.add(0.5), pos2.add(0.5));
        }
        renderer.end();
    }

    @Override
    public @NotNull NBTCompound toNBT() {
        var root = new MutableNBTCompound();
        if (pos1 != null) root.set("pos1", CoordinateUtil.toNBT(pos1));
        if (pos2 != null) root.set("pos2", CoordinateUtil.toNBT(pos2));
        return root.toCompound();
    }

    @Override
    public void fromNBT(@NotNull NBTCompound nbt) {
        pos1 = nbt.contains("pos1") ? CoordinateUtil.fromNBT(nbt.getCompound("pos1")) : null;
        pos2 = nbt.contains("pos2") ? CoordinateUtil.fromNBT(nbt.getCompound("pos2")) : null;
        updateRender();
    }

    @Override
    public void changeSize(int delta, boolean changeVertical, boolean changeHorizontal) {
        throw new UnsupportedOperationException();
    }

}
