package net.hollowcube.terraform.cui.meow;

import net.hollowcube.common.types.Axis;
import net.hollowcube.common.util.ColorUtil;
import net.hollowcube.compat.axiom.AxiomPlayer;
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

        hasPhysics = false;
        setNoGravity(true);
        collidesWithEntities = false;
        setGlowing(true);
        setPositionInternal(player.getPosition().withView(0, 0));
        final ItemDisplayMeta meta = this.getEntityMeta();
        final Vec vec = Vec.fromPoint(to.sub(from));

        final var scale = getVec(vec);

        meta.setInvisible(true);
        meta.setTranslation(from.sub(player.getPosition()).add(scale.div(2)));
        meta.setScale(scale);
        meta.setItemStack(ItemStack.builder(Material.DIAMOND)
                .set(DataComponents.ITEM_MODEL, "mapmaker:colored_cube")
                .set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(color)))
                .build());
        meta.setGlowColorOverride(ColorUtil.toRgb(color));
    }

    private @NotNull Vec getVec(Vec vec) {
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
            case X -> new Vec(vec.x() + THICKNESS, THICKNESS, THICKNESS);
            case Y -> new Vec(THICKNESS, vec.y() + THICKNESS, THICKNESS);
            case Z -> new Vec(THICKNESS, THICKNESS, vec.z() + THICKNESS);
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
