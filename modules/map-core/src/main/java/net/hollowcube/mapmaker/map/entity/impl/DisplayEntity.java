package net.hollowcube.mapmaker.map.entity.impl;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.util.NbtUtil;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.nbt.BinaryTagSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.kyori.adventure.nbt.FloatBinaryTag.floatBinaryTag;

@SuppressWarnings("UnstableApiUsage")
public sealed class DisplayEntity extends MapEntity permits DisplayEntity.Block, DisplayEntity.Item, DisplayEntity.Text {
    private static final BinaryTagSerializer<AbstractDisplayMeta.BillboardConstraints> BILLBOARD_CONSTRAINTS = BinaryTagSerializer.fromEnumStringable(AbstractDisplayMeta.BillboardConstraints.class);

    public static final Tag<UUID> SELECTED_DISPLAY_ENTITY = Tag.Transient("mapmaker:selected_display_entity");

    protected DisplayEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);

        setNoGravity(true);
        hasPhysics = false;
    }

    @Override
    protected void movementTick() {
        // Intentionally do nothing
    }

    @Override
    public @NotNull AbstractDisplayMeta getEntityMeta() {
        return (AbstractDisplayMeta) super.getEntityMeta();
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        final AbstractDisplayMeta meta = getEntityMeta();
        if (meta.getTransformationInterpolationStartDelta() != 0)
            tag.putInt("start_interpolation", meta.getTransformationInterpolationStartDelta());
        if (meta.getTransformationInterpolationDuration() != 0)
            tag.putInt("interpolation_duration", meta.getTransformationInterpolationDuration());
        if (meta.getPosRotInterpolationDuration() != 0)
            tag.putInt("teleport_duration", meta.getPosRotInterpolationDuration());

        // Write transformation as decomposed form always.
        var transformationTag = CompoundBinaryTag.builder();
        var translation = meta.getTranslation();
        if (!translation.isZero()) {
            var elems = List.<BinaryTag>of(floatBinaryTag((float) translation.x()), floatBinaryTag((float) translation.y()), floatBinaryTag((float) translation.z()));
            transformationTag.put("translation", ListBinaryTag.listBinaryTag(BinaryTagTypes.FLOAT, elems));
        }
        var scale = meta.getScale();
        if (!scale.isZero()) {
            var elems = List.<BinaryTag>of(floatBinaryTag((float) scale.x()), floatBinaryTag((float) scale.y()), floatBinaryTag((float) scale.z()));
            transformationTag.put("scale", ListBinaryTag.listBinaryTag(BinaryTagTypes.FLOAT, elems));
        }
        var leftRotation = meta.getLeftRotation();
        if (leftRotation[0] != 0 || leftRotation[1] != 0 || leftRotation[2] != 0 || leftRotation[3] != 0) {
            var elems = List.<BinaryTag>of(floatBinaryTag(leftRotation[0]), floatBinaryTag(leftRotation[1]), floatBinaryTag(leftRotation[2]), floatBinaryTag(leftRotation[3]));
            transformationTag.put("left_rotation", ListBinaryTag.listBinaryTag(BinaryTagTypes.FLOAT, elems));
        }
        var rightRotation = meta.getRightRotation();
        if (rightRotation[0] != 0 || rightRotation[1] != 0 || rightRotation[2] != 0 || rightRotation[3] != 0) {
            var elems = List.<BinaryTag>of(floatBinaryTag(rightRotation[0]), floatBinaryTag(rightRotation[1]), floatBinaryTag(rightRotation[2]), floatBinaryTag(rightRotation[3]));
            transformationTag.put("right_rotation", ListBinaryTag.listBinaryTag(BinaryTagTypes.FLOAT, elems));
        }
        var transformation = transformationTag.build();
        if (transformation.size() > 0) tag.put("transformation", transformation);

        if (meta.getBillboardRenderConstraints() != AbstractDisplayMeta.BillboardConstraints.FIXED)
            tag.put("billboard", BILLBOARD_CONSTRAINTS.write(meta.getBillboardRenderConstraints()));
        if (meta.getBrightnessOverride() >= 0) {
            tag.put("brightness", CompoundBinaryTag.builder()
                    .putInt("block", (meta.getBrightnessOverride() >> 4) & 0xF)
                    .putInt("sky", (meta.getBrightnessOverride() >> 20) & 0xF)
                    .build());
        }
        if (meta.getViewRange() != 0)
            tag.putFloat("view_range", meta.getViewRange());
        if (meta.getShadowRadius() != 0)
            tag.putFloat("shadow_radius", meta.getShadowRadius());
        if (meta.getShadowStrength() != 0)
            tag.putFloat("shadow_strength", meta.getShadowStrength());
        if (meta.getWidth() != 0)
            tag.putFloat("width", meta.getWidth());
        if (meta.getHeight() != 0)
            tag.putFloat("height", meta.getHeight());
        if (meta.getGlowColorOverride() != 0)
            tag.putInt("glow_color_override", meta.getGlowColorOverride());
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        final AbstractDisplayMeta meta = getEntityMeta();
        if (tag.get("start_interpolation") instanceof NumberBinaryTag startInterpolation)
            meta.setTransformationInterpolationStartDelta(startInterpolation.intValue());
        if (tag.get("interpolation_duration") instanceof NumberBinaryTag interpolationDuration)
            meta.setTransformationInterpolationDuration(interpolationDuration.intValue());
        if (tag.get("teleport_duration") instanceof NumberBinaryTag teleportDuration)
            meta.setPosRotInterpolationDuration(teleportDuration.intValue());
        var transformationTag = tag.get("transformation");
        if (transformationTag instanceof CompoundBinaryTag transform) {
            if (transform.get("translation") instanceof ListBinaryTag translate && translate.size() == 3 && translate.elementType() == BinaryTagTypes.FLOAT)
                meta.setTranslation(new Vec(translate.getFloat(0), translate.getFloat(1), translate.getFloat(2)));
            if (transform.get("scale") instanceof ListBinaryTag scale && scale.size() == 3 && scale.elementType() == BinaryTagTypes.FLOAT)
                meta.setScale(new Vec(scale.getFloat(0), scale.getFloat(1), scale.getFloat(2)));
            if (transform.get("left_rotation") instanceof ListBinaryTag leftRot && leftRot.size() == 4 && leftRot.elementType() == BinaryTagTypes.FLOAT)
                meta.setLeftRotation(new float[]{leftRot.getFloat(0), leftRot.getFloat(1), leftRot.getFloat(2), leftRot.getFloat(3)});
            if (transform.get("right_rotation") instanceof ListBinaryTag rightRot && rightRot.size() == 4 && rightRot.elementType() == BinaryTagTypes.FLOAT)
                meta.setRightRotation(new float[]{rightRot.getFloat(0), rightRot.getFloat(1), rightRot.getFloat(2), rightRot.getFloat(3)});
        } else if (transformationTag instanceof ListBinaryTag transformation && transformation.size() == 16 && transformation.elementType() == BinaryTagTypes.FLOAT) {
            // https://github.com/apache/commons-math/blob/ffcdf39f8fa7ccd19c5c21a73fccb90c753592cd/commons-math-legacy/src/main/java/org/apache/commons/math4/legacy/linear/SingularValueDecomposition.java#L53
            throw new UnsupportedOperationException("SVD is not supported yet.");
        }
        if (tag.get("billboard") instanceof StringBinaryTag billboardName)
            meta.setBillboardRenderConstraints(BILLBOARD_CONSTRAINTS.read(billboardName));
        if (tag.get("brightness") instanceof CompoundBinaryTag brightnessTag) {
            int brightness = (brightnessTag.getInt("block") << 4)
                    | (brightnessTag.getInt("sky") << 20);
            meta.setBrightnessOverride(brightness);
        } else {
            meta.setBrightnessOverride(-1);
        }
        if (tag.get("view_range") instanceof NumberBinaryTag viewRange)
            meta.setViewRange(viewRange.floatValue());
        if (tag.get("shadow_radius") instanceof NumberBinaryTag shadowRadius)
            meta.setShadowRadius(shadowRadius.floatValue());
        if (tag.get("shadow_strength") instanceof NumberBinaryTag shadowStrength)
            meta.setShadowStrength(shadowStrength.floatValue());
        if (tag.get("width") instanceof NumberBinaryTag width)
            meta.setWidth(width.floatValue());
        if (tag.get("height") instanceof NumberBinaryTag height)
            meta.setHeight(height.floatValue());
        if (tag.get("glow_color_override") instanceof NumberBinaryTag glowColor)
            meta.setGlowColorOverride(glowColor.intValue());
    }

    @Override
    public void sendPacketToViewersAndSelf(@NotNull SendablePacket packet) {
        if (packet instanceof EntityMetaDataPacket(int entity, Map<Integer, Metadata.Entry<?>> entries)) {
            var glowingPacket = new EntityMetaDataPacket(entity, OpUtils.copyAndEdit(entries, it -> {
                byte current = (Byte) it.getOrDefault(MetadataDef.HAS_GLOWING_EFFECT.index(), Metadata.Byte((byte) 0)).value();
                it.put(MetadataDef.HAS_GLOWING_EFFECT.index(), Metadata.Byte((byte) (current | (byte) 0x40)));
            }));

            for (Player viewer : this.getViewers()) {
                if (this.getUuid().equals(viewer.getTag(SELECTED_DISPLAY_ENTITY))) {
                    viewer.sendPacket(glowingPacket);
                } else {
                    viewer.sendPacket(packet);
                }
            }
        } else {
            super.sendPacketToViewersAndSelf(packet);
        }
    }

    /**
     * Sends a metadata update to all viewers.
     */
    public void forceSendMetaPacket() {
        var flags = this.metadata.get(MetadataDef.ENTITY_FLAGS);
        this.metadata.set(MetadataDef.ENTITY_FLAGS, flags);
    }

    /**
     * Translates the entity by the given point.
     */
    public void translateDisplay(Point point) {
        getEntityMeta().setTranslation(new Vec(point.x(), point.y(), point.z()));
    }

    /**
     * Rotates the entity by the given point but keeping it self-centered on its translation.
     */
    public void rotateDisplay(Point point) {
        var left = Quaternion.fromEulerAngles(point);
        getEntityMeta().setLeftRotation(left.into());
        getEntityMeta().setRightRotation(new float[]{0, 0, 0, 1});

        double x = left.getX(), y = left.getY(), z = left.getZ(), w = left.getW();

        double n = 1.0 / Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)));
        double qx = x * n, qy = y * n, qz = z * n, qw = w * n;
        double xx = qx * qx, yy = qy * qy, zz = qz * qz, ww = qw * qw;
        double xy = qx * qy, xz = qx * qz, yz = qy * qz, xw = qx * qw;
        double zw = qz * qw, yw = qy * qw;
        double k = 1.0 / (xx + yy + zz + ww);

        var translation = getEntityMeta().getTranslation();
        getEntityMeta().setTranslation(new Vec(
                Math.fma(2 * (xz + yw) * k, translation.x(), Math.fma(2 * (yz - xw) * k, translation.y(), ((zz - xx - yy + ww) * k) * translation.z())),
                Math.fma(2 * (xy - zw) * k, translation.x(), Math.fma((yy - xx - zz + ww) * k, translation.y(), (2 * (yz + xw) * k) * translation.z())),
                Math.fma((xx - yy - zz + ww) * k, translation.x(), Math.fma(2 * (xy + zw) * k, translation.y(), (2 * (xz - yw) * k) * translation.z()))
        ));
    }

    /**
     * Scales the entity by the given point but keeping it self-centered on its translation.
     */
    public void scaleDisplay(Point point) {
        Point scale = getEntityMeta().getScale();
        Point translation = getEntityMeta().getTranslation();
        double x = translation.x() / scale.x(), y = translation.y() / scale.y(), z = translation.z() / scale.z();
        getEntityMeta().setTranslation(new Vec(x * point.x(), y * point.y(), z * point.z()));
        getEntityMeta().setScale(new Vec(point.x(), point.y(), point.z()));
    }

    public static final class Block extends DisplayEntity {

        public Block(@NotNull UUID uuid) {
            super(EntityType.BLOCK_DISPLAY, uuid);
        }

        @Override
        public @NotNull BlockDisplayMeta getEntityMeta() {
            return (BlockDisplayMeta) super.getEntityMeta();
        }

        @Override
        public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
            super.writeData(tag);

            final BlockDisplayMeta meta = getEntityMeta();
            final net.minestom.server.instance.block.Block block = meta.getBlockStateId();
            if (!block.isAir())
                tag.put("block_state", NbtUtil.BLOCK_COMPOUND.write(block));

        }

        @Override
        public void readData(@NotNull CompoundBinaryTag tag) {
            super.readData(tag);

            final BlockDisplayMeta meta = getEntityMeta();
            if (tag.get("block_state") instanceof CompoundBinaryTag blockState) {
                var block = NbtUtil.BLOCK_COMPOUND.read(blockState);
                if (!block.isAir()) meta.setBlockState(block);
            }
        }

    }

    public static final class Item extends DisplayEntity {
        private static final BinaryTagSerializer<ItemDisplayMeta.DisplayContext> DISPLAY_CONTEXT = BinaryTagSerializer.fromEnumStringable(ItemDisplayMeta.DisplayContext.class);

        public Item(@NotNull UUID uuid) {
            super(EntityType.ITEM_DISPLAY, uuid);
        }

        @Override
        public @NotNull ItemDisplayMeta getEntityMeta() {
            return (ItemDisplayMeta) super.getEntityMeta();
        }

        @Override
        public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
            super.writeData(tag);

            final ItemDisplayMeta meta = getEntityMeta();
            var itemStack = meta.getItemStack();
            if (!itemStack.isAir() && itemStack.amount() > 0)
                tag.put("item", NbtUtil.writeItemStack(itemStack));
            if (meta.getDisplayContext() != ItemDisplayMeta.DisplayContext.NONE)
                tag.put("item_display", DISPLAY_CONTEXT.write(meta.getDisplayContext()));
        }

        @Override
        public void readData(@NotNull CompoundBinaryTag tag) {
            super.readData(tag);

            final ItemDisplayMeta meta = getEntityMeta();
            if (tag.get("item") instanceof CompoundBinaryTag item)
                meta.setItemStack(NbtUtil.readItemStack(item));
            if (tag.get("item_display") instanceof StringBinaryTag itemDisplay)
                meta.setDisplayContext(DISPLAY_CONTEXT.read(itemDisplay));

        }

    }

    public static final class Text extends DisplayEntity {
        private static final int DEFAULT_BACKGROUND = 1073741824;

        public Text(@NotNull UUID uuid) {
            super(EntityType.TEXT_DISPLAY, uuid);
        }

        @Override
        public @NotNull TextDisplayMeta getEntityMeta() {
            return (TextDisplayMeta) super.getEntityMeta();
        }

        @Override
        public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
            super.writeData(tag);

            final TextDisplayMeta meta = getEntityMeta();
            tag.putString("text", GsonComponentSerializer.gson().serialize(meta.getText()));
            if (meta.getLineWidth() != 0)
                tag.putInt("line_width", meta.getLineWidth());
            if (meta.getBackgroundColor() != DEFAULT_BACKGROUND)
                tag.putInt("background", meta.getBackgroundColor());
            if (meta.getTextOpacity() != 0)
                tag.putByte("text_opacity", (byte) meta.getTextOpacity());
            if (meta.isShadow())
                tag.putByte("shadow", (byte) 1);
            if (meta.isSeeThrough())
                tag.putByte("see_through", (byte) 1);
            if (meta.isUseDefaultBackground())
                tag.putByte("default_background", (byte) 1);
            if (meta.isAlignLeft()) tag.putString("alignment", "left");
            else if (meta.isAlignRight()) tag.putString("alignment", "right");
            else tag.putString("alignment", "center");
        }

        @Override
        public void readData(@NotNull CompoundBinaryTag tag) {
            super.readData(tag);

            final TextDisplayMeta meta = getEntityMeta();
            if (tag.get("text") instanceof StringBinaryTag text)
                meta.setText(GsonComponentSerializer.gson().deserialize(text.value()));
            if (tag.get("line_width") instanceof NumberBinaryTag lineWidth)
                meta.setLineWidth(lineWidth.intValue());
            if (tag.get("background") instanceof NumberBinaryTag background)
                meta.setBackgroundColor(background.intValue());
            if (tag.get("text_opacity") instanceof NumberBinaryTag textOpacity)
                meta.setTextOpacity(textOpacity.byteValue());
            if (tag.get("shadow") instanceof NumberBinaryTag shadow)
                meta.setShadow(shadow.byteValue() != 0);
            if (tag.get("see_through") instanceof NumberBinaryTag seeThrough)
                meta.setSeeThrough(seeThrough.byteValue() != 0);
            if (tag.get("default_background") instanceof NumberBinaryTag defaultBackground)
                meta.setUseDefaultBackground(defaultBackground.byteValue() != 0);
            if (tag.get("alignment") instanceof StringBinaryTag align) {
                // Center gets neither set
                meta.setAlignLeft("left".equals(align.value()));
                meta.setAlignRight("right".equals(align.value()));
            }
        }

    }
}
