package net.hollowcube.mapmaker.chat.announcements;

import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.adventure.audience.Audiences;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ChatAnnouncer {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void setupAnnouncements(@NotNull ConfigLoaderV3 configLoader) {
        var config = configLoader.get(AnnouncementsConfig.class);
        var announcements = config.items();

        setupAnnouncementTasks(announcements);
    }

    private static void setupAnnouncementTasks(List<Announcement> announcements) {
        for (Announcement announcement : announcements) {
            scheduler.scheduleAtFixedRate(() -> announce(announcement), announcement.interval(),
                    announcement.interval(), TimeUnit.SECONDS);
        }
    }

    private static void announce(@NotNull Announcement announcement) {
        Component message = MiniMessage.miniMessage().deserialize(announcement.message());
        Audiences.players().sendMessage(message);
    }
}
