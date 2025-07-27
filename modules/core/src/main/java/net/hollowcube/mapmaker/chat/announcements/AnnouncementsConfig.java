package net.hollowcube.mapmaker.chat.announcements;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RuntimeGson
public record AnnouncementsConfig(int interval, @NotNull List<Announcement> messages) {
}
