package net.hollowcube.mapmaker.session;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record SessionStateUpdateRequest(
    @Nullable Boolean hidden,

    Metadata metadata
) {

    public static SessionStateUpdateRequest hidden(boolean newValue, boolean silent) {
        return new SessionStateUpdateRequest(newValue, new Metadata(silent));
    }

    @RuntimeGson
    public record Metadata(@Nullable Boolean hideSilent) {

        public Metadata() {
            this(null);
        }
    }
}
