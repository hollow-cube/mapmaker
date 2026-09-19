package net.hollowcube.mapmaker.map.entity.impl.other;

import net.hollowcube.common.physics.Shapes;
import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.BlockTags;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.color.DyeColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.CushionMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static net.minestom.server.coordinate.CoordConversion.globalToBlock;

public class CushionEntity extends MapEntity<CushionMeta> {

    public static final MapEntityInfo<CushionEntity> INFO = MapEntityInfo.<CushionEntity>builder(MapEntity.INFO)
        .with("Color", MapEntityInfoType.Enum(DyeColor.class, DyeColor.WHITE, CushionMeta::setColor, CushionMeta::getColor))
        .build();

    private static final String COLOR_KEY = "color";

    private static final double BOTTOM_OFFSET = 1 / 64.0;
    private static final double SUPPORT_REACH = 1 / 8.0;
    /// How far past the clicked point the retrace runs, so the click is inside the segment rather
    /// than exactly on its end.
    private static final double RETRACE_OVERSHOOT = 0.001;

    public CushionEntity(UUID uuid) {
        super(EntityType.CUSHION, uuid);

        hasPhysics = false;
        setNoGravity(true);
    }

    public DyeColor color() {
        return getEntityMeta().getColor();
    }

    public void setColor(DyeColor color) {
        getEntityMeta().setColor(color);
    }

    public Material material() {
        return Objects.requireNonNull(Material.fromKey(dyed("cushion")));
    }

    @Override
    public void onBuildLeftClick(MapWorld world, Player player) {
        playSound(SoundEvent.ENTITY_CUSHION_BREAK, 1, 1);

        var wool = Objects.requireNonNull(Block.fromKey(dyed("wool")));
        var box = getBoundingBox();
        sendPacketToViewers(new ParticlePacket(
            Particle.BLOCK.withBlock(wool),
            position.x(), position.y() + box.height() * 2.0 / 3, position.z(),
            (float) box.width() / 4f, (float) box.height() / 4f, (float) box.depth() / 4f,
            0.05f, 10
        ));

        remove();
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        hasPhysics = false;
        setNoGravity(true);

        setColor(NbtUtilV2.readStringEnum(tag.get(COLOR_KEY), DyeColor.class));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        tag.put(COLOR_KEY, NbtUtilV2.writeStringEnum(color()));
    }

    private String dyed(String suffix) {
        return color().name().toLowerCase(Locale.ROOT) + "_" + suffix;
    }

    //region Placement

    /// Where a cushion placed by clicking `face` of the block at `blockPosition` comes to rest: the
    /// middle of that block's top, at the height clicked, turned to the nearest quarter of a turn.
    /// Null if it would not stay there ([#canRest]). Occupancy by other cushions is not checked.
    ///
    /// @param clickPosition the absolute position of the click on the clicked block
    public static @Nullable Pos placementPosition(
        Block.Getter blocks, Point eyePosition, float yaw,
        Point blockPosition, BlockFace face, Point clickPosition
    ) {
        var clicked = blocks.getBlock(blockPosition, Block.Getter.Condition.TYPE);
        if (BlockTags.CUSHION_USES_COLLISION_SHAPE.contains(clicked.key())) {
            // The client reports the face of the interaction shape, which for these blocks caps their hollow top.
            // Retrace against the collision shape so a cushion rests on the real surface inside.
            var ray = clickPosition.sub(eyePosition).asVec();
            var rayEnd = clickPosition.add(ray.normalize().mul(RETRACE_OVERSHOOT));
            var hit = Shapes.clip(Shapes.boxes(clicked.collisionShape(), blockPosition), eyePosition, rayEnd);
            if (hit != null) {
                face = hit.face();
                clickPosition = hit.position();
            }
        }
        if (face != BlockFace.TOP) return null;

        var snappedYaw = Math.round(yaw / 90f) * 90f;
        var position = new Pos(blockPosition.blockX() + 0.5, clickPosition.y(), blockPosition.blockZ() + 0.5, snappedYaw, 0);
        return canRest(blocks, restingBox(position)) ? position : null;
    }

    public static boolean isOccupied(Instance instance, Point position) {
        var box = EntityType.CUSHION.boundingBox();
        for (var entity : instance.getNearbyEntities(position, 2)) {
            if (entity instanceof CushionEntity && !entity.isRemoved() && box.intersectEntity(position, entity))
                return true;
        }
        return false;
    }

    static BoundingBox restingBox(Point position) {
        return Shapes.absolute(position, EntityType.CUSHION.boundingBox());
    }

    /// Whether a cushion occupying `box` stays there: something under it holds it up, the sliver it
    /// rests in is not filled in by the blocks it sits among, and it is not wholly inside blocks
    /// that would smother it.
    static boolean canRest(Block.Getter blocks, BoundingBox box) {
        return backed(blocks, box) && !filled(blocks, box) && !smothered(blocks, box);
    }

    /// Any block outline reaching into the sliver under the cushion holds it up. The sliver stops an
    /// ulp short on its upper corners, so a neighbour a cushion merely abuts is not what holds it.
    private static boolean backed(Block.Getter blocks, BoundingBox box) {
        var underside = BoundingBox.fromPoints(
            new Vec(box.minX(), box.minY() - BOTTOM_OFFSET, box.minZ()),
            new Vec(Math.nextDown(box.maxX()), box.minY(), Math.nextDown(box.maxZ()))
        );
        for (int x = globalToBlock(underside.minX()); x <= globalToBlock(underside.maxX()); x++) {
            for (int y = globalToBlock(underside.minY() - SUPPORT_REACH); y <= globalToBlock(underside.maxY()); y++) {
                for (int z = globalToBlock(underside.minZ()); z <= globalToBlock(underside.maxZ()); z++) {
                    var shape = blocks.getBlock(x, y, z, Block.Getter.Condition.TYPE).outlineShape();
                    var outline = Shapes.bounds(shape, new Vec(x, y, z));
                    if (outline != null && Shapes.intersects(outline, underside)) return true;
                }
            }
        }
        return false;
    }

    /// Whether block collision leaves the resting sliver no room at all, which is a cushion pushed
    /// into the floor rather than laid on it.
    private static boolean filled(Block.Getter blocks, BoundingBox box) {
        var sliver = Shapes.deflated(BoundingBox.fromPoints(
            new Vec(box.minX(), box.minY(), box.minZ()),
            new Vec(box.maxX(), box.minY() + BOTTOM_OFFSET, box.maxZ()))
        );
        var collision = new ArrayList<BoundingBox>();
        // One extra layer below for collision shapes taller than a block (fences, walls)
        for (int x = globalToBlock(sliver.minX()); x <= globalToBlock(sliver.maxX()); x++) {
            for (int y = globalToBlock(sliver.minY()) - 1; y <= globalToBlock(sliver.maxY()); y++) {
                for (int z = globalToBlock(sliver.minZ()); z <= globalToBlock(sliver.maxZ()); z++) {
                    var shape = blocks.getBlock(x, y, z, Block.Getter.Condition.TYPE).collisionShape();
                    collision.addAll(Shapes.boxes(shape, new Vec(x, y, z)));
                }
            }
        }
        return Shapes.covers(collision, sliver);
    }

    /// Whether every block the cushion overlaps smothers what is inside it.
    private static boolean smothered(Block.Getter blocks, BoundingBox box) {
        var deflated = Shapes.deflated(box);
        for (int x = globalToBlock(deflated.minX()); x <= globalToBlock(deflated.maxX()); x++) {
            for (int y = globalToBlock(deflated.minY()); y <= globalToBlock(deflated.maxY()); y++) {
                for (int z = globalToBlock(deflated.minZ()); z <= globalToBlock(deflated.maxZ()); z++) {
                    if (!BlockUtil.isSuffocating(blocks.getBlock(x, y, z, Block.Getter.Condition.TYPE))) return false;
                }
            }
        }
        return true;
    }

    //endregion
}
