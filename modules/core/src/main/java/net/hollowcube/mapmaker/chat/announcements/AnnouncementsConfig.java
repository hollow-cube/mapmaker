package net.hollowcube.mapmaker.chat.announcements;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;

@ConfigSerializable
public record AnnouncementsConfig(int interval, @NotNull List<Announcement> messages) {
}
