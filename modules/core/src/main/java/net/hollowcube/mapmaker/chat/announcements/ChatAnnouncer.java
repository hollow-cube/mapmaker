package net.hollowcube.mapmaker.chat.announcements;

import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.util.Shutdowner;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class ChatAnnouncer {
    private static final Tag<Integer> LAST_ANNOUNCED_INDEX = Tag.Integer("mapmaker:announcements/last_announced_index");

    private static final Logger logger = LoggerFactory.getLogger(ChatAnnouncer.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final SessionManager sessionManager;
    private final AnnouncementsConfig config;

    public static void setupAnnouncements(@NotNull ConfigLoaderV3 configLoader, @NotNull SessionManager sessionManager, Shutdowner shutdowner) {
        var announcer = new ChatAnnouncer(configLoader, sessionManager);
        shutdowner.queue("ChatAnnouncer", announcer::shutdown);
    }

    private ChatAnnouncer(@NotNull ConfigLoaderV3 configLoader, @NotNull SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.config = configLoader.get(AnnouncementsConfig.class);

        if (this.config.messages().isEmpty()) {
            logger.warn("no announcements configured");
            return;
        }

        this.setupAnnouncementTask();
    }

    private void setupAnnouncementTask() {
        Runnable announceTask = () -> {
            for (var player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                var announcement = this.selectRandomAnnouncement(player);
                if (announcement == null) {
                    continue;
                }

                this.announce(announcement, player);
            }
        };

        var interval = this.config.interval();
        scheduler.scheduleAtFixedRate(announceTask, interval, interval, TimeUnit.SECONDS);
    }

    private @Nullable Announcement selectRandomAnnouncement(@NotNull Player player) {
        var messages = this.config.messages();
        if (messages.size() == 1) return messages.get(0);

        var session = this.sessionManager.getSession(player.getUuid().toString());
        if (session == null) {
            logger.warn("player {} has no session", player.getUsername());
            return null;
        }

        Integer lastAnnouncedIndex = player.getTag(LAST_ANNOUNCED_INDEX);
        if (lastAnnouncedIndex == null) {
            lastAnnouncedIndex = -1;
        }

        var randomIndex = this.randomIndex();
        var announcement = messages.get(randomIndex);

        while (!this.isAnnouncementValidForSession(announcement, session) || randomIndex == lastAnnouncedIndex) {
            randomIndex = this.randomIndex();
            announcement = messages.get(randomIndex);
        }

        player.setTag(LAST_ANNOUNCED_INDEX, randomIndex);
        return announcement;
    }

    private int randomIndex() {
        return ThreadLocalRandom.current().nextInt(this.config.messages().size());
    }

    private void announce(@NotNull Announcement announcement, @NotNull Player player) {
        player.sendMessage(Component.translatable(announcement.key()));
    }

    private boolean isAnnouncementValidForSession(@NotNull Announcement announcement, @NotNull PlayerSession session) {
        var filters = announcement.filters();
        return filters == null || filters.matches(session);
    }

    private void shutdown() {
        scheduler.shutdownNow();
    }
}
