package net.hollowcube.compat.axiom.packets.clientbound;

import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.data.AxiomCapabilities;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

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
            boolean canUseBlueprints,
            boolean canuseEditor,
            boolean canEditDisplayEntities,
            boolean canCreateAnnotations,
            @MagicConstant(valuesFromClass = AxiomCapabilities.class) int allowedCapabilities,
            int infiniteReachLimit,
            int sectionsRateLimit,

            Pair<BlockVec, BlockVec> bounds
    ) {

        private static final NetworkBuffer.Type<Pair<BlockVec, BlockVec>> BOUNDS_SERIALIZER = NetworkBufferTemplate.template(
                NetworkBuffer.VAR_INT, it -> 1,
                NetworkBuffer.BLOCK_POSITION, Pair::left,
                NetworkBuffer.BLOCK_POSITION, Pair::right,
                ($, start, end) -> Pair.of(new BlockVec(start), new BlockVec(end))
        );

        public static final NetworkBuffer.Type<Restrictions> SERIALIZER = NetworkBufferTemplate.template(
                NetworkBuffer.BOOLEAN, Restrictions::canUseBlueprints,
                NetworkBuffer.BOOLEAN, Restrictions::canuseEditor,
                NetworkBuffer.BOOLEAN, Restrictions::canEditDisplayEntities,
                NetworkBuffer.BOOLEAN, Restrictions::canCreateAnnotations,
                NetworkBuffer.INT, Restrictions::allowedCapabilities,
                NetworkBuffer.INT, Restrictions::infiniteReachLimit,
                NetworkBuffer.VAR_INT, Restrictions::sectionsRateLimit,
                BOUNDS_SERIALIZER, Restrictions::bounds,
                Restrictions::new
        );
    }
}
