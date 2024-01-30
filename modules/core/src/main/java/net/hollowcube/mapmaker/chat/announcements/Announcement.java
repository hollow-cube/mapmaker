package net.hollowcube.mapmaker.chat.announcements;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record Announcement(@NotNull String key, @Nullable AnnouncementFilters filters) {
}
