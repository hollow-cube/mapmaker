package net.hollowcube.mapmaker.api.maps;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.RuntimeGson;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/// Indicates the creation or deletion of a map world.
///
/// Note that in case of server crash or isolate destruction, a DESTROYED message is NOT guaranteed to be sent.
/// Therefore, this message should not be used as the SOT for map worlds being removed.
@RuntimeGson
public record MapWorldMessage(
    Action action,
    String worldId,

    // Create-specific fields
    @Nullable String mapId,
    @Nullable String type, // eg editing, playing, verifying

    // Server metadata
    String serverId,
    String serverVersion,
    int protocolVersion
) {
    public enum Action {
        CREATED,
        DESTROYED,
        ;
    }

    public static MapWorldMessage created(String worldId, String mapId, String type) {
        return new MapWorldMessage(Action.CREATED, worldId, mapId, type);
    }

    public static MapWorldMessage destroyed(String worldId) {
        return new MapWorldMessage(Action.DESTROYED, worldId, null, null);
    }

    public MapWorldMessage(Action action, String worldId, @Nullable String mapId, @Nullable String type) {
        var runtime = ServerRuntime.getRuntime();
        this(action, worldId, mapId, type,
            runtime.hostname(), runtime.version(),
            MinecraftServer.PROTOCOL_VERSION);
    }

    public String subject() {
        return "map-world." + action.name().toLowerCase();
    }

}
