package net.hollowcube.anticheat;

import net.hollowcube.anticheat.protocol.PacketTable;
import net.hollowcube.anticheat.protocol.Protocol776;
import net.hollowcube.anticheat.protocol.Protocol777;
import net.hollowcube.anticheat.protocol.S2CEntityPositionSync;
import net.hollowcube.anticheat.protocol.S2CLevelChunkWithLight;
import net.hollowcube.anticheat.state.EntityTypes;
import org.jetbrains.annotations.Nullable;

/// A client protocol version the capture pipeline knows how to read and write: its packet table,
/// the registry ids the model has to recognise, the width a chunk section's global palette is
/// stored at (`ceillog2` of the block state count, which 26.3 pushed past 2^15), and how to write
/// the `entity_position_sync` a capture's prelude places entities with.
public record Protocol(
    int pvn, PacketTable packets, EntityTypes entityTypes, int directBlockBits,
    S2CEntityPositionSync.AtRest entityPositionSync
) {

    /// 26.2.
    public static final int PVN_776 = 776;
    /// 26.3.
    public static final int PVN_777 = 777;

    public static final Protocol V776 = new Protocol(PVN_776, Protocol776.PACKETS, EntityTypes.V776,
        S2CLevelChunkWithLight.V776.DIRECT_BLOCK_BITS, S2CEntityPositionSync.V776::atRest);
    public static final Protocol V777 = new Protocol(PVN_777, Protocol777.PACKETS, EntityTypes.V777,
        S2CLevelChunkWithLight.V777.DIRECT_BLOCK_BITS, S2CEntityPositionSync.V777::atRest);

    public static boolean isSupported(int pvn) {
        return find(pvn) != null;
    }

    private static @Nullable Protocol find(int pvn) {
        return switch (pvn) {
            case PVN_776 -> V776;
            case PVN_777 -> V777;
            default -> null;
        };
    }

    public static Protocol of(int pvn) {
        var protocol = find(pvn);
        if (protocol == null) throw new IllegalArgumentException("unsupported client protocol " + pvn);
        return protocol;
    }
}
