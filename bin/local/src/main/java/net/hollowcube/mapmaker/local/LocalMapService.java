package net.hollowcube.mapmaker.local;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.editor.EditState;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.SaveStateUpdateRequest;
import net.hollowcube.mapmaker.map.SaveStateUpdateResponse;
import net.hollowcube.mapmaker.misc.noop.NoopMapService;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class LocalMapService extends NoopMapService {
    private final Path mapDirectory;

    private final Path worldFile;
    private final Path playerData;

    public LocalMapService(Path mapDirectory) {
        this.mapDirectory = mapDirectory;

        this.worldFile = mapDirectory.resolve("world.polar");
        this.playerData = mapDirectory.resolve("playerdata");
    }

    @Override
    public byte @Nullable [] getMapWorld(String id, boolean write) {
        try {
            if (!Files.exists(worldFile)) return null;
            return Files.readAllBytes(worldFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateMapWorld(String id, byte[] worldData) {
        try {
            Files.write(mapDirectory.resolve("world.polar"), worldData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SaveState getLatestSaveState(
        String mapId, String playerId,
        @Nullable SaveStateType type, SaveStateType.@Nullable Serializer<?> serializer
    ) {
        Check.notNull(type, "type cannot be null");
        Check.notNull(serializer, "serializer cannot be null");

        try {
            EditState editState;
            var saveStatePath = playerData.resolve(playerId);
            if (Files.exists(saveStatePath)) {
                JsonObject object = new Gson().fromJson(Files.readString(saveStatePath), JsonObject.class);
                editState = (EditState) serializer.codec().decode(Transcoder.JSON, object).orElseThrow();
            } else editState = new EditState();

            return new SaveState(
                UUID.randomUUID().toString(), playerId, mapId,
                type, serializer, editState
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable SaveStateUpdateResponse updateSaveState(
        String mapId, String playerId,
        String id, SaveStateUpdateRequest update
    ) {
        var updates = update.updates();
        if (!updates.has("editState")) return null;

        var saveStatePath = playerData.resolve(playerId);
        try {
            Files.createDirectories(saveStatePath.getParent());
            Files.writeString(saveStatePath, new Gson().toJson(updates.get("editState")));
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
