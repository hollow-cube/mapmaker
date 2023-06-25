package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Blocking
public interface MapService {

    /**
     * Creates a new map in the map service with the given owner.
     *
     * @param authorizer The player authorizing the request to the map service.
     * @param owner      The player who will own the newly created map.
     * @return The created map
     */
    @NotNull MapData createMap(@NotNull String authorizer, @NotNull String owner);

    @NotNull MapSearchResponse searchMaps(@NotNull String authorizer, int page, @NotNull String query);

    @NotNull MapData getMap(@NotNull String authorizer, @NotNull String id);

    void updateMap(@NotNull String authorizer, @NotNull String id, @NotNull MapUpdateRequest update);

    void deleteMap(@NotNull String authorizer, @NotNull String id);

    @NotNull MapData publishMap(@NotNull String authorizer, @NotNull String id);

    byte @Nullable [] getMapWorld(@NotNull String id, boolean write);
    void updateMapWorld(@NotNull String id, byte @NotNull [] worldData);

    // Save states
    @NotNull SaveState createSaveState(@NotNull String mapId, @NotNull String playerId);
    @NotNull SaveState getSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id);
    @NotNull SaveState getLatestSaveState(@NotNull String mapId, @NotNull String playerId);
    void updateSaveState(@NotNull String mapId, @NotNull String playerId, @NotNull String id, @NotNull SaveStateUpdateRequest update);

    class NotFoundError extends RuntimeException {
        public NotFoundError(@NotNull String id) {
            super("Map not found: " + id);
        }
    }

    class InternalError extends RuntimeException {
        public InternalError(@NotNull String message) {
            super(message);
        }

        public InternalError(Throwable cause) {
            super(cause);
        }
    }
}
