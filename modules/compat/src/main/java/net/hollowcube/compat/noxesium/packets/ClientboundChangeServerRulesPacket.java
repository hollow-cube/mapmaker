package net.hollowcube.compat.noxesium.packets;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.noxesium.NoxesiumAPI;
import net.hollowcube.compat.noxesium.rules.NoxesiumServerRule;
import net.hollowcube.compat.noxesium.rules.NoxesiumServerRules;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ClientboundChangeServerRulesPacket(
        Map<NoxesiumServerRule<?>, Object> rules
) implements ClientboundModPacket<ClientboundChangeServerRulesPacket> {

    public static final Type<ClientboundChangeServerRulesPacket> TYPE = Type.of(
            NoxesiumAPI.CHANNEL, "change_server_rules",
            // TODO figure out why bazel infer cant actually infer properly here
            new NetworkBuffer.Type<ClientboundChangeServerRulesPacket>() {

                @Override
                public void write(@NotNull NetworkBuffer buffer, ClientboundChangeServerRulesPacket packet) {
                    buffer.write(NetworkBuffer.VAR_INT_ARRAY, packet.rules.keySet().stream().mapToInt(NoxesiumServerRule::id).toArray());
                    packet.rules.forEach((rule, value) -> rule.codec().write(buffer, cast(value)));
                }

                @Override
                public ClientboundChangeServerRulesPacket read(@NotNull NetworkBuffer buffer) {
                    int[] ids = buffer.read(NetworkBuffer.VAR_INT_ARRAY);
                    Map<NoxesiumServerRule<?>, Object> rules = new HashMap<>(ids.length);
                    for (int id : ids) {
                        NoxesiumServerRule<?> rule = NoxesiumServerRules.byId(id);
                        rules.put(rule, rule.codec().read(buffer));
                    }
                    return new ClientboundChangeServerRulesPacket(rules);
                }
            }
    );

    @Override
    public Type<ClientboundChangeServerRulesPacket> getType() {
        return TYPE;
    }

    // I hate java
    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static ClientboundChangeServerRulesPacket itemNameOffset(int offset) {
        return new ClientboundChangeServerRulesPacket(Map.of(NoxesiumServerRules.HELD_ITEM_NAME_OFFSET, offset));
    }

    public static ClientboundChangeServerRulesPacket creativeTab(List<ItemStack> items) {
        return new ClientboundChangeServerRulesPacket(Map.of(NoxesiumServerRules.CREATIVE_TAB, items));
    }
}
