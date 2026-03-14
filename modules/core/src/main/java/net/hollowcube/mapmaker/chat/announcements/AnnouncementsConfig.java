package net.hollowcube.mapmaker.chat.announcements;

import net.hollowcube.common.util.RuntimeGson;

import java.util.List;

@RuntimeGson
public record AnnouncementsConfig(int interval, List<Announcement> messages) {
}
