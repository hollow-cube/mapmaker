package net.hollowcube.terraform.entity;

import net.hollowcube.mapmaker.map.entity.MapEntities;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.test.ServerTest;
import net.hollowcube.test.TestEnv;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ServerTest
class TerraformEntityTest {

    static {
        MapEntities.init(EventNode.type("ignored", EventFilter.INSTANCE));
    }

    @Test
    void testWriteToTagSingleEntity(TestEnv env) {
        var instance = env.createEmptyInstance();

        var entity = MapEntityType.create(EntityType.ITEM_DISPLAY, UUID.randomUUID());
        entity.setInstance(instance, Pos.ZERO);
        var tfEntity = assertInstanceOf(TerraformEntity.class, entity);

        var tag = TerraformEntity.writeToTagWithPassengers(tfEntity);
        assertEquals("minecraft:item_display", tag.getString("id"));
        assertEquals(0, tag.getList("Passengers").size());
    }

    @Test
    void testWriteToTagEntityWithPassengers(TestEnv env) {
        var instance = env.createEmptyInstance();

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
    void testWriteToTagEntityExcludeNonTFPassengers(TestEnv env) {
        var instance = env.createEmptyInstance();

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
