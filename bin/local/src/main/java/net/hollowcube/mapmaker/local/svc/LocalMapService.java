package net.hollowcube.mapmaker.local.svc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.hollowcube.mapmaker.local.LocalServerRunner;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.world.savestate.EditState;
import net.hollowcube.mapmaker.misc.noop.NoopMapService;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.dfu.DFU;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class LocalMapService extends NoopMapService {
    private final Path worldFile;
    private final Path mapFile;
    private final Path savestatePath;
    private final Path perfdumpPath;

    public LocalMapService(@NotNull Path workspace) {
        this.worldFile = workspace.resolve("world.polar");
        this.mapFile = workspace.resolve("map.json");
        this.savestatePath = workspace.resolve(".savestates");
        this.perfdumpPath = workspace.resolve(".perfdump");
    }

    @Override
    public @NotNull String terraformStorageName() {
        return "LocalTerraformStorage";
    }

    @Override
    public @NotNull MapData getMap(@NotNull String authorizer, @NotNull String id) {
        Check.argCondition(!id.equals(LocalServerRunner.DUMMY_MAP_ID), "invalid map id: " + id);

        var settings = new MapSettings();
        settings.setVariant(MapVariant.PARKOUR);
        settings.setSize(MapSize.UNLIMITED);
        var map = new MapData(LocalServerRunner.DUMMY_MAP_ID, UUID.randomUUID().toString(), settings, -1, null);
        map.setSetting(MapSettings.LIGHTING, true);

        if (Files.exists(mapFile)) {
            try {
                var updates = AbstractHttpService.GSON.fromJson(Files.readString(mapFile), MapUpdateRequest.class);
                settings.setName(updates.name);
                if (updates.spawnPoint != null) settings.setSpawnPoint(updates.spawnPoint);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return map;
    }

    @Override
    public byte @Nullable [] getMapWorld(@NotNull String id, boolean write) {
        Check.argCondition(!id.equals(LocalServerRunner.DUMMY_MAP_ID), "invalid map id: " + id);
        try {
            return Files.exists(worldFile) ? Files.readAllBytes(worldFile) : null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateMap(@NotNull String authorizer, @NotNull String id, @NotNull MapUpdateRequest update) {
        try {
            JsonObject object = Files.exists(mapFile)
                    ? AbstractHttpService.GSON.fromJson(Files.readString(mapFile), JsonObject.class)
                    : new JsonObject();
            AbstractHttpService.GSON.toJsonTree(update).getAsJsonObject()
                    .entrySet().forEach(entry -> object.add(entry.getKey(), entry.getValue()));
            Files.createDirectories(mapFile.getParent());
            Files.writeString(mapFile, AbstractHttpService.GSON.toJson(object),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateMapWorld(@NotNull String id, byte @NotNull [] worldData) {
        Check.argCondition(!id.equals(LocalServerRunner.DUMMY_MAP_ID), "invalid map id: " + id);
        try {
            Files.createDirectories(worldFile.getParent());
            Files.write(worldFile, worldData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull SaveState getLatestSaveState(
            @NotNull String mapId, @NotNull String playerId,
            @Nullable SaveStateType type, SaveStateType.@Nullable Serializer<?> serializer
    ) {
        Check.argCondition(!mapId.equals(LocalServerRunner.DUMMY_MAP_ID), "invalid map id: " + mapId);
        Check.notNull(type, "type cannot be null");
        Check.notNull(serializer, "serializer cannot be null");

        try {
            EditState editState;
            var saveStatePath = savestatePath.resolve(playerId);
            if (Files.exists(saveStatePath)) {
                JsonObject object = new Gson().fromJson(Files.readString(saveStatePath), JsonObject.class);
                editState = (EditState) DFU.unwrap(serializer.codec().decode(JsonOps.INSTANCE, object)).getFirst();
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
            @NotNull String mapId, @NotNull String playerId,
            @NotNull String id, @NotNull SaveStateUpdateRequest update
    ) {
        var updates = update.updates();
        if (!updates.has("editState")) return null;

        var saveStatePath = savestatePath.resolve(playerId);
        try {
            Files.createDirectories(saveStatePath.getParent());
            Files.writeString(saveStatePath, new Gson().toJson(updates.get("editState")));
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void uploadPerfdump(@NotNull String name, @NotNull Path data) {
        try {
            Files.createDirectories(perfdumpPath);
            Files.write(perfdumpPath.resolve(name), Files.readAllBytes(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
