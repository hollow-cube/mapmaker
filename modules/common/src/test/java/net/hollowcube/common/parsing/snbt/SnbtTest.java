package net.hollowcube.common.parsing.snbt;

import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SnbtTest {

    @Test
    void preservesMixedFloatAndIntListTypes() {
        var compound = (CompoundBinaryTag) Snbt.parse("{Pose:[24f,336,0f]}");
        var pose = compound.getList("Pose");

        assertEquals(BinaryTagTypes.LIST_WILDCARD, pose.elementType());
        assertEquals(24F, assertInstanceOf(FloatBinaryTag.class, pose.get(0)).value());
        assertEquals(336, assertInstanceOf(IntBinaryTag.class, pose.get(1)).value());
        assertEquals(0F, assertInstanceOf(FloatBinaryTag.class, pose.get(2)).value());
    }

    @Test
    void preservesMixedIntAndFloatListTypesRegardlessOfOrder() {
        var compound = (CompoundBinaryTag) Snbt.parse("{Pose:[24,336f,0]}");
        var pose = compound.getList("Pose");

        assertEquals(BinaryTagTypes.LIST_WILDCARD, pose.elementType());
        assertEquals(24, assertInstanceOf(IntBinaryTag.class, pose.get(0)).value());
        assertEquals(336F, assertInstanceOf(FloatBinaryTag.class, pose.get(1)).value());
        assertEquals(0, assertInstanceOf(IntBinaryTag.class, pose.get(2)).value());
    }

    @Test
    void preservesMixedIntegerTypes() {
        var compound = (CompoundBinaryTag) Snbt.parse("{values:[1b,2s,3]}");
        var values = compound.getList("values");

        assertEquals(BinaryTagTypes.LIST_WILDCARD, values.elementType());
        assertEquals(1, assertInstanceOf(ByteBinaryTag.class, values.get(0)).value());
        assertEquals(2, assertInstanceOf(ShortBinaryTag.class, values.get(1)).value());
        assertEquals(3, assertInstanceOf(IntBinaryTag.class, values.get(2)).value());
    }

    @Test
    void preservesMixedNumericAndStringTypes() {
        var compound = (CompoundBinaryTag) Snbt.parse("{values:[1,\"two\"]}");
        var values = compound.getList("values");

        assertEquals(BinaryTagTypes.LIST_WILDCARD, values.elementType());
        assertEquals(1, assertInstanceOf(IntBinaryTag.class, values.get(0)).value());
        assertEquals("two", assertInstanceOf(StringBinaryTag.class, values.get(1)).value());
    }
}
