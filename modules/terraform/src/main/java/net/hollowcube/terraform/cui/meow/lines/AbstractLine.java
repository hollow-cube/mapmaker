package net.hollowcube.terraform.cui.meow.lines;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.ColorUtil;
import net.hollowcube.compat.axiom.AxiomPlayer;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import net.minestom.server.network.packet.server.play.BundlePacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public abstract class AbstractLine extends Entity {
    public static final double THICKNESS = 0.025;
    @NotNull
    private final Player player;

    public AbstractLine(Player player, Point from, Point to, RGBLike color) {
        super(EntityType.ITEM_DISPLAY, UUID.randomUUID());
        this.player = player;

        this.setInstance(player.getInstance());
        this.addViewer(player);
        this.updateViewableRule();

        this.hasPhysics = false;
        this.setNoGravity(true);
        this.collidesWithEntities = false;
        this.setGlowing(true);
        this.setPositionInternal(player.getPosition().withView(0, 0));

        this.reshape(from, to);
        this.recolor(color);

        getEntityMeta().setTransformationInterpolationDuration(4);

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

    public void recolor(RGBLike color) {
        final ItemDisplayMeta meta = this.getEntityMeta();

        meta.setGlowColorOverride(ColorUtil.toRgb(color));
        meta.setItemStack(ItemStack.builder(Material.DIAMOND)
                .set(DataComponents.ITEM_MODEL, "mapmaker:colored_cube")
                .set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(color)))
                .build());
    }

    public void reshape(Point from, Point to) {
        player.sendPacket(new BundlePacket());

        final ItemDisplayMeta meta = this.getEntityMeta();
        var vec = Vec.fromPoint(to.sub(from));
        var length = vec.length();

        var scale = new Vec(length + THICKNESS, THICKNESS, THICKNESS);
        setPositionInternal(Pos.fromPoint(from));

        //setPositionInternal(player.getPreviousPosition().withView(0,0).sub(player.getVelocity()));
        meta.setPosRotInterpolationDuration(2);
        meta.setTransformationInterpolationDuration(2);
        meta.setTransformationInterpolationStartDelta(0);
        //player.sendPacket(new EntityTeleportPacket(this.getEntityId(), player.getPosition().withView(0,0), Vec.ZERO, RelativeFlags.DELTA_COORD, true));
        this.synchronizePosition();
        meta.setTranslation(vec.div(2));

        meta.setInvisible(true);
        meta.setLeftRotation(Quaternion.fromEulerAngles(CoordinateUtil.getAnglesFromPoints(from, to)).normalizeThis().into());
        meta.setRightRotation(new float[]{0, 0, 0, 1});
        meta.setScale(scale);
        player.sendPacket(new BundlePacket());
    }

    @Override
    public void remove() {
        removeViewer(player);
        this.updateViewableRule();
        super.remove();
    }
}
