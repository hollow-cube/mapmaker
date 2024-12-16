package net.hollowcube.mapmaker.chat.announcements;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AnnouncementsConfig(int interval, @NotNull List<Announcement> messages) {
}
