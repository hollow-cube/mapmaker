package net.hollowcube.mapmaker.map.entity;

import net.hollowcube.terraform.entity.TerraformEntity;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@EnvTest
class TerraformEntityTest {

    static {
        MapEntities.init(EventNode.type("ignored", EventFilter.INSTANCE));
    }

    @Test
    void testWriteToTagSingleEntity(Env env) {
        var instance = env.createFlatInstance();

        var entity = MapEntityType.create(EntityType.ITEM_DISPLAY, UUID.randomUUID());
        entity.setInstance(instance, Pos.ZERO);
        var tfEntity = assertInstanceOf(TerraformEntity.class, entity);

        var tag = TerraformEntity.writeToTagWithPassengers(tfEntity);
        assertEquals("minecraft:item_display", tag.getString("id"));
        assertEquals(0, tag.getList("Passengers").size());
    }

    @Test
    void testWriteToTagEntityWithPassengers(Env env) {
        assumeFalse(true, "TODO: fix this");
        var instance = env.createFlatInstance();

        var entity = MapEntityType.create(EntityType.ITEM_DISPLAY, UUID.randomUUID());
        entity.setInstance(instance, Pos.ZERO);
        var passenger1 = MapEntityType.create(EntityType.BLOCK_DISPLAY, UUID.randomUUID());
        passenger1.setInstance(instance, Pos.ZERO).thenRun(() -> entity.addPassenger(passenger1));
        var passenger2 = MapEntityType.create(EntityType.TEXT_DISPLAY, UUID.randomUUID());
        passenger2.setInstance(instance, Pos.ZERO).thenRun(() -> entity.addPassenger(passenger2));

        var tfEntity = assertInstanceOf(TerraformEntity.class, entity);
        var tag = TerraformEntity.writeToTagWithPassengers(tfEntity);
        assertEquals("minecraft:item_display", tag.getString("id"));

        var passengers = tag.getList("Passengers", BinaryTagTypes.COMPOUND);
        assertEquals(2, passengers.size());

        var passenger1Tag = passengers.getCompound(0);
        assertEquals("minecraft:block_display", passenger1Tag.getString("id"));
        var passenger2Tag = passengers.getCompound(1);
        assertEquals("minecraft:text_display", passenger2Tag.getString("id"));
    }

    @Test
    void testWriteToTagEntityExcludeNonTFPassengers(Env env) {
        assumeFalse(true);
        var instance = env.createFlatInstance();

        var entity = MapEntityType.create(EntityType.ITEM_DISPLAY, UUID.randomUUID());
        entity.setInstance(instance, Pos.ZERO);
        var passenger1 = MapEntityType.create(EntityType.BLOCK_DISPLAY, UUID.randomUUID());
        passenger1.setInstance(instance, Pos.ZERO).thenRun(() -> entity.addPassenger(passenger1));
        var passenger2 = new Entity(EntityType.TEXT_DISPLAY, UUID.randomUUID());
        passenger2.setInstance(instance, Pos.ZERO).thenRun(() -> entity.addPassenger(passenger2));

        var tfEntity = assertInstanceOf(TerraformEntity.class, entity);
        var tag = TerraformEntity.writeToTagWithPassengers(tfEntity);
        assertEquals("minecraft:item_display", tag.getString("id"));

        var passengers = tag.getList("Passengers", BinaryTagTypes.COMPOUND);
        assertEquals(1, passengers.size());

        var passenger1Tag = passengers.getCompound(0);
        assertEquals("minecraft:block_display", passenger1Tag.getString("id"));
    }
}
