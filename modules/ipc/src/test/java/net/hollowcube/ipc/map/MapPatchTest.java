package net.hollowcube.ipc.map;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MapPatchTest {
    @Test
    void builtPatchesAndRenderedMapsRemainStableAfterFurtherEdits() {
        var original = MapData.draft(UUID.randomUUID(), UUID.randomUUID());
        var builder = new MapPatch.Builder(original);
        var value = new JsonObject();
        value.addProperty("nested", 1);
        builder.setExtra("custom", value);
        builder.addTag("future_tag");
        var patch = builder.build();
        var snapshot = builder.map();
        value.addProperty("nested", 2);
        builder.setName("Later");
        builder.removeTag("future_tag");
        builder.setExtra("other", value);

        assertEquals(1, patch.extra().getAsJsonObject("custom").get("nested").getAsInt());
        assertFalse(patch.extra().has("other"));
        assertNull(patch.name());
        assertEquals(List.of("future_tag"), patch.tags());
        assertEquals(original.name(), snapshot.name());
        assertEquals(List.of("future_tag"), snapshot.settings().tags());
        assertTrue(original.settings().extra().isEmpty());
    }

    @Test
    void successfulSaveUsesTheReturnedSnapshotAsTheNextBase() {
        var builder = new MapPatch.Builder(MapData.draft(UUID.randomUUID(), UUID.randomUUID()));
        builder.setName("Local name");
        var server = new MapPatch.Builder(builder.map());
        server.setName("Accepted name");
        server.setProtocolVersion(774);
        builder.save(patch -> server.map());

        assertSame(server.map(), builder.map());
        assertTrue(builder.build().isEmpty());
        builder.setListed(false);
        assertEquals("Accepted name", builder.map().name());
        assertEquals(774, builder.map().protocolVersion());
        assertNull(builder.build().name());
        assertEquals(false, builder.build().listed());
    }

    @Test
    void explicitClearsAndZeroValuesDifferFromUnchangedFields() {
        var builder = new MapPatch.Builder(MapData.draft(UUID.randomUUID(), UUID.randomUUID()));
        builder.setSubVariant("speedrun");
        builder.reset(builder.map());
        assertNull(builder.build().subvariant());
        builder.setSubVariant(null);
        builder.setProtocolVersion(0);
        builder.setListed(false);
        builder.setTags(List.of());

        var patch = builder.build();
        assertEquals("none", patch.subvariant());
        assertEquals(0, patch.protocolVersion());
        assertEquals(false, patch.listed());
        assertEquals(List.of(), patch.tags());
        assertNull(builder.map().settings().subvariant());
        assertFalse(patch.isEmpty());
    }
}
