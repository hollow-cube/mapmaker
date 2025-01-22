package net.hollowcube.compat.noxesium.packets;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.noxesium.NoxesiumAPI;
import net.hollowcube.compat.noxesium.rules.NoxesiumEntityRule;
import net.hollowcube.compat.noxesium.rules.NoxesiumEntityRules;
import net.minestom.server.entity.Entity;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record ClientboundResetEntityRulesPacket(
        int entity,
        List<NoxesiumEntityRule<?>> rules
) implements ClientboundModPacket<ClientboundResetEntityRulesPacket> {

    public static final Type<ClientboundResetEntityRulesPacket> TYPE = Type.of(
            NoxesiumAPI.CHANNEL, "reset_extra_entity_data",
            NetworkBufferTemplate.template(
                    NetworkBuffer.VAR_INT, ClientboundResetEntityRulesPacket::entity,
                    NoxesiumEntityRule.NETWORK_TYPE.list(), ClientboundResetEntityRulesPacket::rules,
                    ClientboundResetEntityRulesPacket::new
            )
    );

    @Override
    public Type<ClientboundResetEntityRulesPacket> getType() {
        return TYPE;
    }

    public static ClientboundResetEntityRulesPacket of(Entity entity, NoxesiumEntityRule<?>... rules) {
        return new ClientboundResetEntityRulesPacket(entity.getEntityId(), List.of(rules));
    }
}
