package net.hollowcube.mapmaker.chat.announcements;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record Announcement(String key, @Nullable AnnouncementFilters filters) {
}
