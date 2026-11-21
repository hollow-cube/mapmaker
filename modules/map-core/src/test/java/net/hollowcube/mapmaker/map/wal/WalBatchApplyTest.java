package net.hollowcube.mapmaker.map.wal;

import net.hollowcube.mapmaker.map.entity.MapEntities;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.mapmaker.map.entity.PhysicsTestEnv;
import net.hollowcube.mapmaker.map.entity.impl.other.ItemFrameEntity;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.terraform.apply.ApplyEngine;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.legacy.TerraformImpl;
import net.hollowcube.terraform.legacy.event.TerraformPreSpawnEntityEvent;
import net.hollowcube.terraform.util.GameDataProvider;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import net.minestom.server.world.DimensionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalBatchApplyTest {
    private static NetworkBuffer.Type<EditBuffer> edits;

    private final List<MapInstance> instances = new ArrayList<>();
    private final List<WalBatch> log = new ArrayList<>();

    @BeforeAll
    static void init() {
        PhysicsTestEnv.init();
        var events = EventNode.type("wal-batch-apply-test", EventFilter.INSTANCE);
        MapEntities.init(events);
        events.addListener(TerraformPreSpawnEntityEvent.class, event -> event.setConstructor(MapEntityType::create));
        MinecraftServer.getGlobalEventHandler().addChild(events);
        edits = EditBuffer.serializer(GameDataProvider.NOOP, MinecraftServer.getBiomeRegistry());
    }

    @AfterEach
    void cleanup() {
        instances.forEach(MapInstance::unload);
    }

    @Test
    void reproducesBlocksEntitiesAndWorldTags() {
        var source = createInstance();
        var writer = writer(source);

        source.setBlock(1, 41, 2, Block.STONE);
        source.setBlock(3, 41, 2, Block.OAK_SIGN.withNbt(CompoundBinaryTag.builder().putString("a", "b").build()));
        var kept = spawn(source, new Pos(1.5, 42, 2.5));
        var removed = spawn(source, new Pos(4.5, 42, 2.5));
        writer.setWorldTag(1, "spawn", StringBinaryTag.stringBinaryTag("first"));
        writer.flush();

        source.setBlock(1, 41, 2, Block.DIRT);
        kept.getEntityMeta().setItem(ItemStack.of(Material.STONE));
        kept.teleport(new Pos(2.5, 43, 2.5, 90, 0)).join();
        kept.update(0);
        removed.remove();
        writer.setWorldTag(1, "spawn", null);
        writer.flush();
        writer.close();

        var target = createInstance();
        var engine = ApplyEngine.builder().entities(TerraformImpl.ENTITIES).build();
        for (var batch : log) batch.apply(engine, target, 0);

        assertEquals(Block.DIRT.stateId(), target.getBlock(1, 41, 2).stateId());
        assertEquals("b", target.getBlock(3, 41, 2).nbt().getString("a"));
        assertNull(target.getTag(Tag.NBT("spawn")));
        assertNull(target.getEntityByUuid(removed.getUuid()));
        var replayed = assertInstanceOf(ItemFrameEntity.class, target.getEntityByUuid(kept.getUuid()));
        assertEquals(kept.getPosition(), replayed.getPosition());
        assertEquals(kept.writeToTag(), replayed.writeToTag());
    }

    @Test
    void skipsRecordsAlreadyInTheWorld() {
        var source = createInstance();
        var writer = writer(source);
        source.setBlock(1, 41, 2, Block.STONE);
        long first = writer.flush();
        source.setBlock(1, 42, 2, Block.DIRT);
        writer.flush();
        writer.close();

        var target = createInstance();
        var engine = ApplyEngine.builder().entities(TerraformImpl.ENTITIES).build();
        for (var batch : log) batch.apply(engine, target, first);

        assertTrue(target.getBlock(1, 41, 2).air());
        assertEquals(Block.DIRT.stateId(), target.getBlock(1, 42, 2).stateId());
    }

    private WalWriter writer(MapInstance instance) {
        return new WalWriter(instance, TerraformImpl.ENTITIES, edits, 0,
            batch -> log.add(WalBatch.decode(NetworkBuffer.wrap(batch.bytes(), 0, batch.bytes().length), edits)),
            () -> {});
    }

    private static ItemFrameEntity spawn(MapInstance instance, Pos pos) {
        var frame = new ItemFrameEntity(UUID.randomUUID());
        frame.setInstance(instance, pos).join();
        return frame;
    }

    private MapInstance createInstance() {
        var instance = new MapInstance("test", DimensionType.OVERWORLD, MapInstance.LightingMode.LOADED);
        instances.add(instance);
        instance.loadChunk(0, 0).join();
        return instance;
    }
}
