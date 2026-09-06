package net.hollowcube.mapmaker.map;

import net.hollowcube.ipc.map.MapPatch;
import com.google.gson.JsonObject;
import net.hollowcube.ipc.map.MapData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MapPatchBuilderTest {
    @Test
    void editsPreserveEarlierSnapshotsAndUnknownSettings() {
        var extra = new JsonObject();
        extra.addProperty("future_setting", 42);
        var defaults = MapData.Settings.defaults();
        var original = MapData.draft(UUID.randomUUID(), UUID.randomUUID()).withSettings(new MapData.Settings(
            "Original", defaults.icon(), defaults.size(), defaults.variant(), defaults.subvariant(),
            defaults.spawnPoint(), List.of("future_tag"), defaults.leaderboard(), extra));
        var editor = new MapPatch.Builder(original);
        editor.setName("Edited");
        MapSettings.set(editor, MapSettings.NO_JUMP, true);

        assertEquals("Original", original.name());
        assertFalse(original.settings().extra().has("no_jump"));
        assertEquals("Edited", editor.map().name());
        assertTrue(MapSettings.get(editor.map().settings(), MapSettings.NO_JUMP));
        assertEquals(42, editor.map().settings().extra().get("future_setting").getAsInt());
        assertEquals(List.of("future_tag"), editor.map().settings().tags());
        editor.save(request -> {
            assertEquals("Edited", request.name());
            assertFalse(request.extra().has("future_setting"));
            assertNull(request.tags());
            return editor.map();
        });
    }

    @Test
    void failedWritesRetainPendingEditsAndSuccessfulWritesClearThem() {
        var editor = new MapPatch.Builder(MapData.draft(UUID.randomUUID(), UUID.randomUUID()));
        editor.setName("Retry me");
        editor.save(request -> null);
        assertThrows(IllegalStateException.class, () -> editor.save(request -> {
            throw new IllegalStateException("write failed");
        }));
        editor.save(request -> {
            assertEquals("Retry me", request.name());
            return editor.map();
        });
        editor.save(request -> fail("successful write should clear pending edits"));
        editor.setName("Next edit");
        editor.save(request -> {
            assertEquals("Next edit", request.name());
            return editor.map();
        });
    }

    @Test
    void editsDuringWriteRemainPendingForNextWrite() throws Exception {
        var editor = new MapPatch.Builder(MapData.draft(UUID.randomUUID(), UUID.randomUUID()));
        editor.setName("First");
        var writing = new CountDownLatch(1);
        var releaseWrite = new CountDownLatch(1);
        var editing = new CountDownLatch(1);
        var sent = new AtomicReference<String>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var write = executor.submit(() -> editor.save(request -> {
                sent.set(request.name());
                writing.countDown();
                try {
                    assertTrue(releaseWrite.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return editor.map();
            }));
            assertTrue(writing.await(5, TimeUnit.SECONDS));
            var edit = executor.submit(() -> {
                editing.countDown();
                editor.setName("Second");
            });
            assertTrue(editing.await(5, TimeUnit.SECONDS));
            releaseWrite.countDown();
            write.get(5, TimeUnit.SECONDS);
            edit.get(5, TimeUnit.SECONDS);
        } finally {
            releaseWrite.countDown();
        }
        assertEquals("First", sent.get());
        assertEquals("Second", editor.map().name());
        editor.save(request -> {
            assertEquals("Second", request.name());
            return editor.map();
        });
    }
}
