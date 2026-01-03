package net.hollowcube.mapmaker.notifications.impl;

import net.hollowcube.mapmaker.notifications.PlayerNotification;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface PlayerNotificationType {

    String type();

    @NonBlocking
    PlayerNotification createNotification(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry);

    @NonBlocking
    @Nullable Component createToast(Player player, ServiceContext context, PlayerNotificationResponse.SimpleEntry entry);

    final class Lookup {

        private static final Map<String, PlayerNotificationType> TYPES = ServiceLoader.load(PlayerNotificationType.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toMap(PlayerNotificationType::type, Function.identity()));

        private Lookup() {

        }

        public static @Nullable PlayerNotificationType get(String type) {
            return TYPES.get(type);
        }
    }
}
