package net.hollowcube.terraform.legacy.storage;

import net.hollowcube.terraform.legacy.schem.SchematicHeader;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TerraformStorageMemory implements TerraformStorage {
    private final Map<String, byte[]> sessionData = new ConcurrentHashMap<>();
    private final Map<String, byte[]> localSessionData = new ConcurrentHashMap<>();

    private record Schem(@NotNull SchematicHeader header, byte @NotNull [] data) {
    }

    private final Map<String, List<Schem>> schematics = new ConcurrentHashMap<>();

    public TerraformStorageMemory() {
    }

    @Override
    public byte @Nullable [] loadPlayerSession(@NotNull String playerId) {
        return sessionData.get(playerId);
    }

    @Override
    public void savePlayerSession(@NotNull String playerId, byte @NotNull [] session) {
        sessionData.put(playerId, session);
    }

    @Override
    public byte @Nullable [] loadLocalSession(@NotNull String playerId, @NotNull String instanceId) {
        return localSessionData.get(String.format("%s:%s", playerId, instanceId));
    }

    @Override
    public void saveLocalSession(@NotNull String playerId, @NotNull String instanceId, byte @NotNull [] session) {
        localSessionData.put(String.format("%s:%s", playerId, instanceId), session);
    }

    @Override
    public @NotNull List<@NotNull SchematicHeader> listSchematics(@NotNull String playerId) {
        var playerSchems = schematics.get(playerId);
        if (playerSchems.isEmpty()) return List.of();
        return playerSchems.stream().map(Schem::header).toList();
    }

    @Override
    public byte @Nullable [] loadSchematicData(@NotNull String playerId, @NotNull String name) {
        var playerSchems = schematics.get(playerId);
        if (playerSchems == null) return null;
        return playerSchems.stream().filter(s -> s.header().name().equals(name)).findFirst().map(Schem::data).orElse(null);
    }

    @Override
    public @NotNull SchematicCreateResult createSchematic(@NotNull String playerId, @NotNull String name, byte @NotNull [] data, @NotNull Point dimensions, boolean overwrite) {
        var playerSchems = schematics.computeIfAbsent(playerId, k -> new CopyOnWriteArrayList<>());
        for (var schem : playerSchems) {
            if (schem.header.name().equalsIgnoreCase(name)) {
                if (overwrite) {
                    playerSchems.remove(schem);
                    break;
                } else {
                    return SchematicCreateResult.DUPLICATE_ENTRY;
                }
            }
        }

        if (playerSchems.size() > 5) {
            return SchematicCreateResult.ENTRY_LIMIT_EXCEEDED;
        }

        var header = new SchematicHeader(name, data.length, dimensions, "sponge");
        playerSchems.add(new Schem(header, data));
        return SchematicCreateResult.SUCCESS;
    }

    @Override
    public @NotNull SchematicDeleteResult deleteSchematic(@NotNull String playerId, @NotNull String name) {
        var playerSchems = schematics.get(playerId);
        if (playerSchems == null) return SchematicDeleteResult.NOT_FOUND;

        for (var schem : playerSchems) {
            if (schem.header.name().equalsIgnoreCase(name)) {
                playerSchems.remove(schem);
                return SchematicDeleteResult.SUCCESS;
            }
        }

        return SchematicDeleteResult.NOT_FOUND;
    }

}
