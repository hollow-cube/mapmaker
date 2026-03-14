package net.hollowcube.compat.axiom.properties;

import net.hollowcube.compat.axiom.packets.clientbound.AxiomClientboundSetWorldPropertyPacket;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface PropertyDispatcher {

    PropertyDispatcher INSTANCE = new PropertyDispatcher() {

        private static final Tag<Map<WorldProperty<?>, Object>> PROPERTIES = Tag.<Map<WorldProperty<?>, Object>>Transient("axiom:properties/instance")
            .defaultValue(ConcurrentHashMap::new);

        @Override
        public <T> void set(Player player, WorldProperty<T> property, T value) {
            var properties = player.getInstance().getTag(PROPERTIES);
            properties.put(property, value);
            AxiomClientboundSetWorldPropertyPacket.of(property, value).sendToInstance(player.getInstance());
        }

        @Override
        public <T> T get(Player player, WorldProperty<T> property) {
            var properties = player.getInstance().getTag(PROPERTIES);
            @SuppressWarnings("unchecked")
            var result = (T) properties.getOrDefault(property, property.initialValue);
            return result;
        }
    };

    <T> void set(Player player, WorldProperty<T> property, T value);

    <T> T get(Player player, WorldProperty<T> property);
}
