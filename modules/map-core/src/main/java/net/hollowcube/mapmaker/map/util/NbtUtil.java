package net.hollowcube.mapmaker.map.util;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.nbt.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.nbt.BinaryTagSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.kyori.adventure.text.Component.text;

@SuppressWarnings("UnstableApiUsage")
public final class NbtUtil {

    public static final BinaryTagSerializer<Block> BLOCK_COMPOUND = new BinaryTagSerializer<>() {
        @Override
        public @NotNull BinaryTag write(@NotNull Block value) {
            var defaultProps = Block.fromBlockId(value.id()).properties(); // Get the props of the default state to compare

            var props = CompoundBinaryTag.builder();
            for (var entry : value.properties().entrySet()) {
                if (entry.getValue().equals(defaultProps.get(entry.getKey()))) continue; // Skip default values
                props.put(entry.getKey(), StringBinaryTag.stringBinaryTag(entry.getValue()));
            }
            var propsCompound = props.build();

            var builder = CompoundBinaryTag.builder()
                    .putString("Name", value.name());
            if (propsCompound.size() > 0) builder.put("Properties", propsCompound);
            return builder.build();
        }

        @Override
        public @NotNull Block read(@NotNull BinaryTag tag) {
            if (!(tag instanceof CompoundBinaryTag compound)) return Block.AIR;

            var block = Block.fromNamespaceId(compound.getString("Name"));
            if (block == null) return Block.AIR;

            for (var entry : compound.getCompound("Properties")) {
                if (!(entry.getValue() instanceof StringBinaryTag string)) continue;
                block = block.withProperty(entry.getKey(), string.value());
            }

            return block;
        }
    };

    public static @NotNull BinaryTag into(@NotNull Point vec) {
        return ListBinaryTag.listBinaryTag(BinaryTagTypes.DOUBLE, List.of(
                DoubleBinaryTag.doubleBinaryTag(vec.x()),
                DoubleBinaryTag.doubleBinaryTag(vec.y()),
                DoubleBinaryTag.doubleBinaryTag(vec.z())
        ));
    }

    public static @Nullable Vec from(@Nullable BinaryTag nbt) {
        if (!(nbt instanceof ListBinaryTag list)) return null;
        return from(list);
    }

    public static @NotNull Vec from(@NotNull ListBinaryTag list) {
        double x = 0, y = 0, z = 0;
        if (list.size() >= 1) x = ((DoubleBinaryTag) list.get(0)).value();
        if (list.size() >= 2) y = ((DoubleBinaryTag) list.get(1)).value();
        if (list.size() >= 3) z = ((DoubleBinaryTag) list.get(2)).value();
        return new Vec(x, y, z);
    }

    public static @NotNull Pos readRotation(@NotNull Point pos, ListBinaryTag rot) {
        float yaw = 0, pitch = 0;
        if (rot.size() >= 1) yaw = ((FloatBinaryTag) rot.get(0)).value();
        if (rot.size() >= 2) pitch = ((FloatBinaryTag) rot.get(1)).value();
        return new Pos(pos, yaw, pitch);
    }

    public static @NotNull BinaryTag writeRotation(@NotNull Pos pos) {
        return ListBinaryTag.listBinaryTag(BinaryTagTypes.FLOAT, List.of(
                FloatBinaryTag.floatBinaryTag(pos.yaw()),
                FloatBinaryTag.floatBinaryTag(pos.pitch())
        ));
    }

    public static <E extends Enum<E>> @NotNull E readEnum(@NotNull CompoundBinaryTag tag, @NotNull String key, @NotNull E defaultValue) {
        return switch (tag.get(key)) {
            case StringBinaryTag string -> Enum.valueOf(defaultValue.getDeclaringClass(), string.value().toUpperCase());
            case NumberBinaryTag number -> defaultValue.getDeclaringClass().getEnumConstants()[number.intValue()];
            case null, default -> defaultValue;
        };
    }

    public static @NotNull BinaryTag writeItemStack(@NotNull ItemStack itemStack) {
        var tag = (CompoundBinaryTag) ItemStack.NBT_TYPE.write(itemStack);
        var cmdId = BadSprite.getCmdId(itemStack.get(ItemComponent.CUSTOM_MODEL_DATA));
        if (cmdId != null) tag = tag.putString("custom_model_data", cmdId);
        return tag;
    }

    public static @NotNull ItemStack readItemStack(@NotNull BinaryTag tag) {
        if (!(tag instanceof CompoundBinaryTag compound)) return ItemStack.AIR;
        var itemStack = ItemStack.NBT_TYPE.read(compound);
        if (compound.get("custom_model_data") instanceof StringBinaryTag cmdId) {
            int cmd = BadSprite.getIdCmd(cmdId.value());
            if (cmd > 0) itemStack = itemStack.with(ItemComponent.CUSTOM_MODEL_DATA, cmd);
        }
        return itemStack;
    }

    public static @NotNull Component prettyPrint(@NotNull BinaryTag tag) {
        return switch (tag) {
            case EndBinaryTag ignored -> text("END");
            case ByteBinaryTag byteTag ->
                    text(byteTag.value(), NamedTextColor.GOLD).append(text("b", NamedTextColor.RED));
            case ShortBinaryTag shortTag ->
                    text(shortTag.value(), NamedTextColor.GOLD).append(text("s", NamedTextColor.RED));
            case IntBinaryTag intTag -> text(intTag.value(), NamedTextColor.GOLD);
            case LongBinaryTag longTag ->
                    text(longTag.value(), NamedTextColor.GOLD).append(text("L", NamedTextColor.RED));
            case FloatBinaryTag floatTag ->
                    text(floatTag.value(), NamedTextColor.GOLD).append(text("f", NamedTextColor.RED));
            case DoubleBinaryTag doubleTag -> text(doubleTag.value(), NamedTextColor.GOLD);
            case ByteArrayBinaryTag byteArrayTag -> {
                var builder = text();
                builder.append(text("[", NamedTextColor.WHITE), text("B"), text(";", NamedTextColor.WHITE));
                for (int i = 0; i < byteArrayTag.value().length; i++) {
                    builder.append(text(byteArrayTag.value()[i], NamedTextColor.GOLD), text("b", NamedTextColor.RED));
                    if (i < byteArrayTag.value().length - 1) builder.append(text(",", NamedTextColor.WHITE));
                }
                builder.append(text("]", NamedTextColor.WHITE));
                yield builder.build();
            }
            case StringBinaryTag stringTag -> text().append(text('"', NamedTextColor.WHITE),
                    text(stringTag.value(), NamedTextColor.GREEN), text('"', NamedTextColor.WHITE)).build();
            case ListBinaryTag listTag -> {
                var builder = text();
                builder.append(text("[", NamedTextColor.WHITE));
                for (int i = 0; i < listTag.size(); i++) {
                    builder.append(prettyPrint(listTag.get(i)));
                    if (i < listTag.size() - 1) builder.append(text(", ", NamedTextColor.WHITE));
                }
                builder.append(text("]", NamedTextColor.WHITE));
                yield builder.build();
            }
            case CompoundBinaryTag compoundTag -> {
                var builder = text();
                builder.append(text("{", NamedTextColor.WHITE));
                var iter = compoundTag.iterator();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    builder.append(text(entry.getKey(), NamedTextColor.AQUA), text(": ", NamedTextColor.WHITE), prettyPrint(entry.getValue()));
                    if (iter.hasNext()) builder.append(text(", ", NamedTextColor.WHITE));
                }
                builder.append(text("}", NamedTextColor.WHITE));
                yield builder.build();
            }
            case IntArrayBinaryTag intArrayTag -> {
                var builder = text();
                builder.append(text("[", NamedTextColor.WHITE), text("I"), text(";", NamedTextColor.WHITE));
                for (int i = 0; i < intArrayTag.value().length; i++) {
                    builder.append(text(intArrayTag.value()[i], NamedTextColor.GOLD));
                    if (i < intArrayTag.value().length - 1) builder.append(text(",", NamedTextColor.WHITE));
                }
                builder.append(text("]", NamedTextColor.WHITE));
                yield builder.build();
            }
            case LongArrayBinaryTag longArrayTag -> {
                var builder = text();
                builder.append(text("[", NamedTextColor.WHITE), text("L"), text(";", NamedTextColor.WHITE));
                for (int i = 0; i < longArrayTag.value().length; i++) {
                    builder.append(text(longArrayTag.value()[i], NamedTextColor.GOLD), text("L", NamedTextColor.RED));
                    if (i < longArrayTag.value().length - 1) builder.append(text(",", NamedTextColor.WHITE));
                }
                builder.append(text("]", NamedTextColor.WHITE));
                yield builder.build();
            }
            default -> Component.empty();
        };
    }

    public static <T extends BinaryTag> T deepMerge(@NotNull T left, @NotNull T right) {
        if (left instanceof CompoundBinaryTag leftC && right instanceof CompoundBinaryTag rightC) {
            var builder = CompoundBinaryTag.builder();
            builder.put(leftC);
            for (var entry : rightC) {
                var key = entry.getKey();
                var leftValue = leftC.get(key);
                var rightValue = entry.getValue();
                if (leftValue != null) {
                    builder.put(key, deepMerge(leftValue, rightValue));
                } else {
                    builder.put(key, rightValue);
                }
            }
            //noinspection unchecked
            return (T) builder.build();
        }
        return right;
    }
}
