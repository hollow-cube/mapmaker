package net.hollowcube.compat.axiom.properties;

import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundSetWorldPropertyPacket;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface PropertyDispatcher {

    PropertyDispatcher INSTANCE = new PropertyDispatcher() {

        private static final Tag<Map<WorldProperty<?>, Object>> PROPERTIES = Tag.<Map<WorldProperty<?>, Object>>Transient("axiom:properties/instance")
                .defaultValue(ConcurrentHashMap::new);

        @Override
        public <T> void set(@NotNull Player player, @NotNull WorldProperty<T> property, @NotNull T value) {
            var properties = player.getInstance().getTag(PROPERTIES);
            properties.put(property, value);
            AxiomClientboundSetWorldPropertyPacket.of(property, value).sendToViewers(player.getInstance());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(@NotNull Player player, @NotNull WorldProperty<T> property) {
            var properties = player.getInstance().getTag(PROPERTIES);
            return (T) properties.getOrDefault(property, property.initialValue);
        }
    };

    <T> void set(@NotNull Player player, @NotNull WorldProperty<T> property, @NotNull T value);

    <T> T get(@NotNull Player player, @NotNull WorldProperty<T> property);
}
