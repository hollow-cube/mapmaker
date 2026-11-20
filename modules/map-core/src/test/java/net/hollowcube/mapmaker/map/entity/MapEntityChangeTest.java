package net.hollowcube.mapmaker.map.entity;

import net.hollowcube.mapmaker.map.entity.impl.other.ArmorStandEntity;
import net.hollowcube.mapmaker.map.entity.impl.other.ItemFrameEntity;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.terraform.apply.ChangeTracker;
import net.hollowcube.terraform.entity.EntityEdit;
import net.hollowcube.terraform.legacy.TerraformImpl;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.world.DimensionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MapEntityChangeTest {
    private static final Pos SPAWN = new Pos(1.5, 41, 2.5);

    private MapInstance instance;
    private ChangeTracker tracker;

    @BeforeAll
    static void init() {
        PhysicsTestEnv.init();
    }

    @BeforeEach
    void setup() {
        instance = new MapInstance("test", DimensionType.OVERWORLD, MapInstance.LightingMode.LOADED);
        instance.loadChunk(0, 0).join();
        tracker = ChangeTracker.attach(instance, TerraformImpl.ENTITIES);
    }

    @AfterEach
    void cleanup() {
        instance.unload();
    }

    @Test
    void spawnIsMarked() {
        var frame = spawn();

        var edit = onlyEdit();
        assertEquals(frame.getUuid(), edit.id());
        assertEquals(SPAWN, edit.pos());
        assertNotNull(edit.data());
        assertTrue(drain().isEmpty());
    }

    @Test
    void moveIsMarkedOnTheNextUpdate() {
        var frame = spawn();
        drain();

        frame.update(0);
        assertTrue(drain().isEmpty());

        var moved = SPAWN.add(2, 0, 0).withYaw(90);
        frame.teleport(moved).join();
        frame.update(0);
        assertEquals(moved, onlyEdit().pos());
    }

    @Test
    void readDataIsMarked() {
        var frame = spawn();
        drain();

        frame.readData(frame.writeToTag().putBoolean("Glowing", true));

        var data = onlyEdit().data();
        assertNotNull(data);
        assertTrue(data.nbt().getBoolean("Glowing"));
    }

    @Test
    void removeIsMarkedAsARemoval() {
        var frame = spawn();
        drain();

        frame.remove();

        var edit = onlyEdit();
        assertEquals(frame.getUuid(), edit.id());
        assertNull(edit.data());
    }

    @Test
    void metadataIsMarked() {
        var frame = spawn();
        drain();

        frame.getEntityMeta().setItem(ItemStack.of(Material.STONE));

        assertEquals(frame.getUuid(), onlyEdit().id());
    }

    @Test
    void equipmentAndAttributesAreMarked() {
        var stand = new ArmorStandEntity(UUID.randomUUID());
        stand.setInstance(instance, SPAWN).join();
        drain();

        stand.setEquipment(EquipmentSlot.HELMET, ItemStack.of(Material.STONE));
        assertEquals(stand.getUuid(), onlyEdit().id());

        stand.setAttribute(Attribute.SCALE, 2);
        assertEquals(stand.getUuid(), onlyEdit().id());
    }

    @Test
    void objectTagsAreMarked() {
        var marker = new MarkerEntity(UUID.randomUUID());
        marker.setInstance(instance, SPAWN).join();
        drain();

        marker.setType(Key.key("test:type"));

        assertEquals(marker.getUuid(), onlyEdit().id());
    }

    @Test
    void aPassengerIsPartOfItsVehicle() {
        var vehicle = spawn();
        var passenger = spawn();
        drain();

        vehicle.addPassenger(passenger);
        assertPassengerOf(vehicle, passenger);

        passenger.getEntityMeta().setItem(ItemStack.of(Material.STONE));
        assertPassengerOf(vehicle, passenger);
    }

    @Test
    void removingAVehicleMarksItsPassengers() {
        var vehicle = spawn();
        var passenger = spawn();
        vehicle.addPassenger(passenger);
        drain();

        vehicle.remove();

        var edits = drain();
        assertEquals(2, edits.size());
        for (var edit : edits) {
            if (edit.id().equals(vehicle.getUuid())) assertNull(edit.data());
            else {
                assertEquals(passenger.getUuid(), edit.id());
                assertNotNull(edit.data());
            }
        }
    }

    private void assertPassengerOf(ItemFrameEntity vehicle, ItemFrameEntity passenger) {
        var edits = drain();
        assertEquals(2, edits.size());
        for (var edit : edits) {
            if (edit.id().equals(passenger.getUuid())) assertNull(edit.data());
            else {
                assertEquals(vehicle.getUuid(), edit.id());
                assertNotNull(edit.data());
                assertFalse(edit.data().nbt().getList("Passengers").isEmpty());
            }
        }
    }

    private ItemFrameEntity spawn() {
        var frame = new ItemFrameEntity(UUID.randomUUID());
        frame.setInstance(instance, SPAWN).join();
        return frame;
    }

    private List<EntityEdit> drain() {
        return tracker.drain().entities();
    }

    private EntityEdit onlyEdit() {
        var edits = drain();
        assertEquals(1, edits.size());
        return edits.getFirst();
    }
}
