package net.hollowcube.compat.axiom.packets.clientbound;

import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.data.AxiomPermission;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record AxiomClientboundSetRestrictionsPacket(
    @Nullable Restrictions restrictions
) implements AxiomClientboundModPacket<AxiomClientboundSetRestrictionsPacket> {

    public static final Type<AxiomClientboundSetRestrictionsPacket> TYPE = Type.of(
        AxiomAPI.CHANNEL, "restrictions",
        NetworkBufferTemplate.template(
            Restrictions.SERIALIZER, AxiomClientboundSetRestrictionsPacket::restrictions,
            AxiomClientboundSetRestrictionsPacket::new
        )
    );

    @Override
    public Type<AxiomClientboundSetRestrictionsPacket> getType() {
        return TYPE;
    }

    public record Restrictions(
        Set<AxiomPermission> allowed,
        Set<AxiomPermission> disallowed,
        int infiniteReachLimit,
        Pair<BlockVec, BlockVec> bounds
    ) {

        private static final NetworkBuffer.Type<Pair<BlockVec, BlockVec>> BOUNDS_SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.VAR_INT, _ -> 1,
            NetworkBuffer.BLOCK_POSITION, Pair::left,
            NetworkBuffer.BLOCK_POSITION, Pair::right,
            (_, start, end) -> Pair.of(new BlockVec(start), new BlockVec(end))
        );

        public static final NetworkBuffer.Type<Restrictions> SERIALIZER = NetworkBufferTemplate.template(
            AxiomPermission.TYPE.set(), Restrictions::allowed,
            AxiomPermission.TYPE.set(), Restrictions::disallowed,
            NetworkBuffer.INT, Restrictions::infiniteReachLimit,
            BOUNDS_SERIALIZER, Restrictions::bounds,
            Restrictions::new
        );
    }
}
