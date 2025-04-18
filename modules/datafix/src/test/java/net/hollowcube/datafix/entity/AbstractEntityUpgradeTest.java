package net.hollowcube.datafix.entity;

import net.hollowcube.datafix.AbstractDataFixTest;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.util.ByteArrayValue;
import net.hollowcube.datafix.util.IntArrayValue;
import net.hollowcube.datafix.util.LongArrayValue;
import net.hollowcube.datafix.util.Value;
import net.kyori.adventure.nbt.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

abstract class AbstractEntityUpgradeTest extends AbstractDataFixTest {

    private final BinaryTag tag;

    public AbstractEntityUpgradeTest() {
        var path = "/entity/" + getClass().getSimpleName() + ".snbt";
        try (var is = getClass().getResourceAsStream(path)) {
            var snbt = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
            this.tag = TagStringIOExt.readTag(snbt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected BinaryTag upgrade(int fromVersion, int toVersion) {
        var value = super.upgrade(DataTypes.ENTITY, valueFromTag(tag), fromVersion, toVersion);
        return tagFromValue(value);
    }

    protected CompoundBinaryTag upgradeC(int fromVersion, int toVersion) {
        return assertInstanceOf(CompoundBinaryTag.class, upgrade(fromVersion, toVersion));
    }

    private static Value valueFromTag(BinaryTag binaryTag) {
        return switch (binaryTag) {
            case EndBinaryTag _ -> Value.NULL;
            case ByteBinaryTag tag -> Value.wrap(tag.value());
            case ShortBinaryTag tag -> Value.wrap(tag.value());
            case IntBinaryTag tag -> Value.wrap(tag.value());
            case LongBinaryTag tag -> Value.wrap(tag.value());
            case FloatBinaryTag tag -> Value.wrap(tag.value());
            case DoubleBinaryTag tag -> Value.wrap(tag.value());
            case ByteArrayBinaryTag tag -> Value.wrap(tag.value());
            case StringBinaryTag tag -> Value.wrap(tag.value());
            case ListBinaryTag tag -> {
                var list = Value.emptyList();
                tag.forEach(value -> list.put(valueFromTag(value)));
                yield Value.wrap(list);
            }
            case CompoundBinaryTag tag -> {
                var value = Value.emptyMap();
                tag.forEach((entry) ->
                        value.put(entry.getKey(), valueFromTag(entry.getValue())));
                yield value;
            }
            case IntArrayBinaryTag tag -> Value.wrap(tag.value());
            case LongArrayBinaryTag tag -> Value.wrap(tag.value());
            default -> throw new IllegalStateException("Unexpected value: " + binaryTag);
        };
    }

    private static BinaryTag tagFromValue(Value value) {
        if (value.isNull()) {
            throw new UnsupportedOperationException("cannot convert null to tag, should have been skipped prior");
        } else if (value.isMapLike()) {
            var builder = CompoundBinaryTag.builder();
            value.forEachEntry((k, v) -> {
                if (v.isNull()) return;
                builder.put(k, tagFromValue(v));
            });
            return builder.build();
        } else if (value instanceof ByteArrayValue(byte[] byteArray)) {
            return ByteArrayBinaryTag.byteArrayBinaryTag(byteArray);
        } else if (value instanceof IntArrayValue(int[] intArray)) {
            return IntArrayBinaryTag.intArrayBinaryTag(intArray);
        } else if (value instanceof LongArrayValue(long[] longArray)) {
            return LongArrayBinaryTag.longArrayBinaryTag(longArray);
        } else if (value.isListLike()) {
            var builder = ListBinaryTag.builder();
            for (var v : value) {
                if (v.isNull()) continue;
                builder.add(tagFromValue(v));
            }
            return builder.build();
        } else return switch (value.value()) {
            case null -> EndBinaryTag.endBinaryTag();
            case Byte b -> ByteBinaryTag.byteBinaryTag(b);
            case Short s -> ShortBinaryTag.shortBinaryTag(s);
            case Integer i -> IntBinaryTag.intBinaryTag(i);
            case Long l -> LongBinaryTag.longBinaryTag(l);
            case Float f -> FloatBinaryTag.floatBinaryTag(f);
            case Double d -> DoubleBinaryTag.doubleBinaryTag(d);
            case byte[] bytes -> ByteArrayBinaryTag.byteArrayBinaryTag(bytes);
            case String s -> StringBinaryTag.stringBinaryTag(s);
            case int[] ints -> IntArrayBinaryTag.intArrayBinaryTag(ints);
            case long[] longs -> LongArrayBinaryTag.longArrayBinaryTag(longs);
            default -> throw new IllegalStateException("Unexpected value: " + value.value());
        };
    }
}
