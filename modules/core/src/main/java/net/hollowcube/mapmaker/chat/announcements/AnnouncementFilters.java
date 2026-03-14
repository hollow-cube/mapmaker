package net.hollowcube.mapmaker.chat.announcements;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.session.PlayerSession;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RuntimeGson
public record AnnouncementFilters(
    @Nullable String sessionType,
    @Nullable String sessionState
) {
    private static final Logger logger = LoggerFactory.getLogger(AnnouncementFilters.class);

    public boolean matches(PlayerSession session) {
        var presence = session.presence();

        var result = true;
        if (this.sessionType != null) {
            result = presence.type().equals(this.sessionType);
        }
        if (this.sessionState != null) {
            result = result && presence.state().equals(this.sessionState);
        }
        return result;
    }
}
