package net.hollowcube.mapmaker.map.entity.impl.base;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMaps;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.inventory.EquipmentHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.LazyPacket;
import net.minestom.server.network.packet.server.play.EntityAttributesPacket;
import net.minestom.server.network.packet.server.play.EntityEquipmentPacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.hollowcube.mapmaker.map.util.NbtUtilV2.readItemStack;
import static net.hollowcube.mapmaker.map.util.NbtUtilV2.writeItemStack;

/**
 * Represents a LivingEntity in the game. This class basically reimplements the
 * {@link net.minestom.server.entity.LivingEntity} from Minestom, although this
 * implementation is <b>not</b> (necessarily) thread-safe.
 */
@SuppressWarnings("UnstableApiUsage")
public class AbstractLivingEntity<M extends LivingEntityMeta> extends MapEntity<M> implements EquipmentHandler {

    public static final MapEntityInfo<@NotNull AbstractLivingEntity<? extends LivingEntityMeta>> INFO = MapEntityInfo.<AbstractLivingEntity<? extends LivingEntityMeta>>builder(MapEntity.INFO)
        .with("Scale", MapEntityInfoType.Attribute(Attribute.SCALE))
        .with("On Fire", MapEntityInfoType.Bool(false, LivingEntityMeta::setOnFire, LivingEntityMeta::isOnFire))
        .build();

    private static final String ATTRIBUTES_KEY = "attributes";
    private static final String HEALTH_KEY = "Health";

    // Indices are the ordinal of the EquipmentSlot enum.
    private final ItemStack[] equipment = new ItemStack[EquipmentSlot.values().length];
    private final Object2DoubleMap<Attribute> attributes = new Object2DoubleArrayMap<>(Attribute.values().size());

    protected AbstractLivingEntity(@NotNull EntityType entityType) {
        this(entityType, UUID.randomUUID());
    }

    protected AbstractLivingEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);

        this.getEntityMeta().setSilent(true);
        Arrays.fill(equipment, ItemStack.AIR);
    }

    // There is a lot of Mojang serialized fields missing here, may need to add more later.

    @Override
    public void writeData(@NotNull CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        final CompoundBinaryTag.Builder equipment = CompoundBinaryTag.builder();
        for (final EquipmentSlot slot : EquipmentSlot.values()) {
            final ItemStack itemStack = getEquipment(slot);
            if (itemStack.isAir()) continue;
            equipment.put(slot.nbtName(), writeItemStack(itemStack));
        }
        final CompoundBinaryTag equipmentTag = equipment.build();
        if (!equipmentTag.isEmpty()) tag.put("equipment", equipmentTag);

        // Vanilla
        this.writeAttributes(tag);
        tag.putFloat(HEALTH_KEY, getEntityMeta().getHealth());
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        final CompoundBinaryTag equipmentTag = tag.getCompound("equipment");
        for (final EquipmentSlot slot : EquipmentSlot.values()) {
            final BinaryTag itemTag = equipmentTag.get(slot.nbtName());
            if (itemTag == null) continue;
            setEquipment(slot, readItemStack(itemTag));
        }

        // Vanilla
        this.readAttributes(tag);
        this.getEntityMeta().setHealth(tag.getFloat(HEALTH_KEY, getEntityMeta().getHealth()));
    }

    // EquipmentHandler impl


    @Override
    public @NotNull ItemStack getEquipment(@NotNull EquipmentSlot slot) {
        return equipment[slot.ordinal()];
    }

    @Override
    public void setEquipment(@NotNull EquipmentSlot slot, @NotNull ItemStack itemStack) {
        equipment[slot.ordinal()] = itemStack;
        sendPacketToViewersAndSelf(new LazyPacket(this::getEquipmentsPacket));
    }

    @Override
    public @NotNull EntityEquipmentPacket getEquipmentsPacket() {
        // TODO remove when https://github.com/Minestom/Minestom/pull/3073 gets merged
        return new EntityEquipmentPacket(this.getEntityId(), Map.of(
            EquipmentSlot.MAIN_HAND, getEquipment(EquipmentSlot.MAIN_HAND),
            EquipmentSlot.OFF_HAND, getEquipment(EquipmentSlot.OFF_HAND),

            EquipmentSlot.HELMET, getEquipment(EquipmentSlot.HELMET),
            EquipmentSlot.CHESTPLATE, getEquipment(EquipmentSlot.CHESTPLATE),
            EquipmentSlot.LEGGINGS, getEquipment(EquipmentSlot.LEGGINGS),
            EquipmentSlot.BOOTS, getEquipment(EquipmentSlot.BOOTS),

            EquipmentSlot.BODY, getEquipment(EquipmentSlot.BODY),
            EquipmentSlot.SADDLE, getEquipment(EquipmentSlot.SADDLE)
        ));
    }

    // Attribute stuff

    private void writeAttributes(CompoundBinaryTag.Builder builder) {
        var list = ListBinaryTag.builder();
        Object2DoubleMaps.fastForEach(this.attributes, entry -> {
            var attribute = CompoundBinaryTag.builder();
            attribute.putString("id", entry.getKey().name());
            attribute.putDouble("base", entry.getDoubleValue());

            list.add(attribute.build());
        });

        builder.put(ATTRIBUTES_KEY, list.build());
    }

    private void readAttributes(CompoundBinaryTag tag) {
        var list = tag.getList(ATTRIBUTES_KEY);

        this.attributes.clear();
        for (var attribute : list) {
            if (!(attribute instanceof CompoundBinaryTag compound)) continue;

            var id = compound.getString("id");
            var base = compound.getDouble("base");

            var attr = Attribute.fromKey(id);
            if (attr == null) continue;

            this.attributes.put(attr, base);
        }
    }

    protected EntityAttributesPacket getAttributesPacket() {
        List<EntityAttributesPacket.Property> properties = new ArrayList<>();
        Object2DoubleMaps.fastForEach(this.attributes, entry -> properties.add(
            new EntityAttributesPacket.Property(entry.getKey(), entry.getDoubleValue(), List.of())
        ));
        return new EntityAttributesPacket(getEntityId(), properties);
    }

    public void setAttribute(Attribute attribute, double value) {
        this.attributes.put(attribute, value);
        sendPacketToViewers(new EntityAttributesPacket(getEntityId(), List.of(
            new EntityAttributesPacket.Property(attribute, value, List.of())
        )));
    }

    public double getAttribute(Attribute attribute) {
        return this.attributes.getOrDefault(attribute, attribute.defaultValue());
    }

    // viewer stuff

    @Override
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);
        player.sendPacket(new LazyPacket(this::getEquipmentsPacket));
        player.sendPacket(new LazyPacket(this::getAttributesPacket));
    }

}
