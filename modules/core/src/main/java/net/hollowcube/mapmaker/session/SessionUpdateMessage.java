package net.hollowcube.mapmaker.session;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@RuntimeGson
public record SessionUpdateMessage(
        @NotNull Action action,
        @NotNull String playerId,

        @UnknownNullability PlayerSession session,

        @Nullable SessionStateUpdateRequest.Metadata metadata
) {

    @Override
    public @NotNull SessionStateUpdateRequest.Metadata metadata() {
        return this.metadata == null ? new SessionStateUpdateRequest.Metadata(null) : this.metadata;
    }

    public enum Action {
        CREATE,
        DELETE,
        UPDATE
    }

}
