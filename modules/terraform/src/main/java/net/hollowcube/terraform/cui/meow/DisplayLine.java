package net.hollowcube.terraform.cui.meow;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.types.Axis;
import net.hollowcube.common.util.ColorUtil;
import net.hollowcube.compat.axiom.AxiomPlayer;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class DisplayLine extends Entity {
    public static final double THICKNESS = 0.025;

    public DisplayLine(Player player, Point from, Point to, RGBLike color) {
        super(EntityType.ITEM_DISPLAY, UUID.randomUUID());

        this.hasPhysics = false;
        this.setNoGravity(true);
        this.collidesWithEntities = false;
        this.setGlowing(true);
        this.setPositionInternal(player.getPosition().withView(0, 0));
        final ItemDisplayMeta meta = this.getEntityMeta();
        var vec = Vec.fromPoint(to.sub(from));
        var length = vec.length();

        var scale = new Vec(length + THICKNESS, THICKNESS, THICKNESS);

        meta.setInvisible(true);
        meta.setTranslation(from.sub(player.getPosition()).add(vec.div(2)));
        meta.setLeftRotation(Quaternion.fromEulerAngles(CoordinateUtil.getAnglesFromPoints(from, to)).normalizeThis().into());
        meta.setRightRotation(new float[]{0, 0, 0, 1});
        meta.setScale(scale);
        meta.setItemStack(ItemStack.builder(Material.DIAMOND)
                .set(DataComponents.ITEM_MODEL, "mapmaker:colored_cube")
                .set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(color)))
                .build());
        meta.setGlowColorOverride(ColorUtil.toRgb(color));

        this.setInstance(player.getInstance());
        this.addViewer(player);
        this.updateViewableRule();
    }

    public static DisplayLine axisAligned(Player player, Point from, Point to, RGBLike color) {
        var vec = getVec(Vec.fromPoint(to.sub(from)));
        return new DisplayLine(player, from, from.add(vec), color);
    }

    private static @NotNull Vec getVec(Vec vec) {
        var axis = Axis.X;
        var biggest = Math.abs(vec.x());

        if (Math.abs(vec.y()) > biggest) {
            axis = Axis.Y;
            biggest = Math.abs(vec.y());
        }
        if (Math.abs(vec.z()) > biggest) {
            axis = Axis.Z;
        }

        return switch (axis) {
            case X -> new Vec(vec.x(), 0, 0);
            case Y -> new Vec(0, vec.y(), 0);
            case Z -> new Vec(0, 0, vec.z());
        };
    }

    @Override
    protected void movementTick() {
        // Intentionally do nothing
    }

    @Override
    public @NotNull ItemDisplayMeta getEntityMeta() {
        return (ItemDisplayMeta) super.getEntityMeta();
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);

        AxiomPlayer.updateIgnoredEntities(player, it -> it.add(this.getUuid()));
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);

        AxiomPlayer.updateIgnoredEntities(player, it -> it.remove(this.getUuid()));
    }
}
