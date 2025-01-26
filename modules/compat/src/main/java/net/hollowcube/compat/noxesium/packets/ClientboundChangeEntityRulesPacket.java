package net.hollowcube.compat.noxesium.packets;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.noxesium.NoxesiumAPI;
import net.hollowcube.compat.noxesium.rules.NoxesiumEntityRule;
import net.hollowcube.compat.noxesium.rules.NoxesiumEntityRules;
import net.minestom.server.entity.Entity;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record ClientboundChangeEntityRulesPacket(
        int entity,
        Map<NoxesiumEntityRule<?>, Object> rules
) implements ClientboundModPacket<ClientboundChangeEntityRulesPacket> {

    private static final NetworkBuffer.Type<ClientboundChangeEntityRulesPacket> NETWORK_TYPE = new NetworkBuffer.Type<>() {

        @Override
        public void write(@NotNull NetworkBuffer buffer, ClientboundChangeEntityRulesPacket packet) {
            buffer.write(NetworkBuffer.VAR_INT, packet.entity());
            buffer.write(NetworkBuffer.VAR_INT_ARRAY, packet.rules().keySet().stream().mapToInt(NoxesiumEntityRule::id).toArray());
            packet.rules().forEach((rule, value) -> rule.codec().write(buffer, cast(value)));
        }

        @Override
        public ClientboundChangeEntityRulesPacket read(@NotNull NetworkBuffer buffer) {
            int entity = buffer.read(NetworkBuffer.VAR_INT);
            int[] ids = buffer.read(NetworkBuffer.VAR_INT_ARRAY);
            Map<NoxesiumEntityRule<?>, Object> rules = new HashMap<>(ids.length);
            for (int id : ids) {
                NoxesiumEntityRule<?> rule = NoxesiumEntityRules.byId(id);
                rules.put(rule, rule.codec().read(buffer));
            }
            return new ClientboundChangeEntityRulesPacket(entity, rules);
        }
    };

    public static final Type<ClientboundChangeEntityRulesPacket> TYPE = Type.of(
            NoxesiumAPI.CHANNEL, "change_extra_entity_data",
            NETWORK_TYPE
    );

    @Override
    public Type<ClientboundChangeEntityRulesPacket> getType() {
        return TYPE;
    }

    // I hate java
    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static ClientboundChangeEntityRulesPacket qibDepth(Entity entity, double depth) {
        return new ClientboundChangeEntityRulesPacket(entity.getEntityId(), Map.of(NoxesiumEntityRules.QIB_WIDTH_Z, depth));
    }

    public static ClientboundChangeEntityRulesPacket qibBehaviour(Entity entity, String behaviour) {
        return new ClientboundChangeEntityRulesPacket(entity.getEntityId(), Map.of(NoxesiumEntityRules.QIB_BEHAVIOR, behaviour));
    }
}
