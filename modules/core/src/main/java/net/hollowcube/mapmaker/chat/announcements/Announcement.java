package net.hollowcube.mapmaker.chat.announcements;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record Announcement(@NotNull String message, int interval) {
}
