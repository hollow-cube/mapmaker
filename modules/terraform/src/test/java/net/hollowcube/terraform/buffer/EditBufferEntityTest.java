package net.hollowcube.terraform.buffer;

import net.hollowcube.terraform.entity.EntityEdit;
import net.hollowcube.terraform.util.TypedData;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EditBufferEntityTest {
    private static final TypedData ZOMBIE = new TypedData("minecraft:zombie", CompoundBinaryTag.empty());

    @Test
    void builderKeepsEditsInOrder() {
        var spawn = new EntityEdit(UUID.randomUUID(), new Pos(1, 2, 3), ZOMBIE);
        var remove = EntityEdit.remove(UUID.randomUUID(), new Pos(4, 5, 6));

        var buffer = EditBuffer.builder().addEntityEdit(spawn).addEntityEdit(remove).build();

        assertEquals(List.of(spawn, remove), buffer.entities());
        assertEquals(0, buffer.sectionCount());
    }

    @Test
    void clipToBoxKeepsEditsByTheirPosition() {
        var inside = new EntityEdit(UUID.randomUUID(), new Pos(5.5, 5, 5), ZOMBIE);
        var outside = new EntityEdit(UUID.randomUUID(), new Pos(20, 5, 5), ZOMBIE);
        var removedInside = EntityEdit.remove(UUID.randomUUID(), new Pos(1, 1, 1));
        var removedOutside = EntityEdit.remove(UUID.randomUUID(), new Pos(-1, 1, 1));
        var buffer = EditBuffer.builder()
            .addEntityEdit(inside)
            .addEntityEdit(outside)
            .addEntityEdit(removedInside)
            .addEntityEdit(removedOutside)
            .build();

        assertEquals(List.of(inside, removedInside), buffer.clipToBox(0, 0, 0, 10, 10, 10).entities());
    }

    @Test
    void clipToBoxReturnsTheSameBufferWhenNothingIsClipped() {
        var buffer = EditBuffer.builder()
            .setBlockState(1, 1, 1, 1)
            .addEntityEdit(new EntityEdit(UUID.randomUUID(), new Pos(2, 2, 2), ZOMBIE))
            .build();

        assertSame(buffer, buffer.clipToBox(0, 0, 0, 15, 15, 15));
    }

    @Test
    void sizeCountsEntityNbt() {
        var small = EditBuffer.builder()
            .addEntityEdit(new EntityEdit(UUID.randomUUID(), Pos.ZERO, ZOMBIE))
            .build();
        var tagged = EditBuffer.builder()
            .addEntityEdit(new EntityEdit(
                UUID.randomUUID(),
                Pos.ZERO,
                new TypedData(
                    ZOMBIE.type(),
                    CompoundBinaryTag.builder().putString("name", "x".repeat(1000)).build()
                )
            ))
            .build();

        assertTrue(small.sizeBytes() > 0);
        assertTrue(tagged.sizeBytes() >= small.sizeBytes() + 2000);
    }
}
