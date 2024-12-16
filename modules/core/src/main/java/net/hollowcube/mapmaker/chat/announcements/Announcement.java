package net.hollowcube.mapmaker.chat.announcements;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Announcement(@NotNull String key, @Nullable AnnouncementFilters filters) {
}
