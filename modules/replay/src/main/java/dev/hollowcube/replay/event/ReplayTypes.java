package dev.hollowcube.replay.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;

/// Wire types shared by the replay events.
public final class ReplayTypes {

    /// The number of steps a block is divided into, as in a vanilla movement packet.
    private static final double COORDINATE_SCALE = 4096;
    /// The number of steps a full turn is divided into. A vanilla angle byte uses 256; this is the
    /// same fixed point idea at 256 times the resolution, which is 0.0055 of a degree.
    private static final double ANGLE_SCALE = 65536 / 360d;

    private ReplayTypes() {
    }

    /// A position in 14 bytes rather than 32, for [DeltaMoveEvent].
    ///
    /// Deltas are most of what a recording contains, and a [NetworkBuffer#POS] spends 32 bytes on
    /// three doubles and two floats to describe what is usually a fraction of a block. Coordinates
    /// are quantized to 1/4096 of a block and zig-zag varint encoded, so a delta costs two bytes an
    /// axis. Double mantissas are close to incompressible, so this survives compression rather than
    /// being a saving zstd would have found on its own.
    ///
    /// The view is kept far finer than the angle byte a relative move packet would have carried,
    /// because playback moves an entity by teleport and that synchronizes as an entity position
    /// sync carrying float angles. Two bytes of fixed point holds it to 0.0055 of a degree, which
    /// is finer than a player can express and 256 times what watching one live would have shown.
    ///
    /// Only deltas use it. [AbsoluteMoveEvent] and [SpawnEntityEvent] describe where something
    /// really was rather than how far it moved, and an absolute position is what corrects the
    /// rounding these accumulate, so both keep whole coordinates too; between them they are under
    /// one percent of a recording, which buys exact re-anchoring for nothing.
    ///
    /// This is the counterpart to [NetworkBuffer#LP_VECTOR3], which the same event already uses for
    /// velocity.
    public static final NetworkBuffer.Type<Pos> LP_POS = new NetworkBuffer.Type<>() {
        @Override
        public void write(NetworkBuffer buffer, Pos value) {
            writeCoordinate(buffer, value.x());
            writeCoordinate(buffer, value.y());
            writeCoordinate(buffer, value.z());
            buffer.write(NetworkBuffer.SHORT, writeAngle(value.yaw()));
            buffer.write(NetworkBuffer.SHORT, writeAngle(value.pitch()));
        }

        @Override
        public Pos read(NetworkBuffer buffer) {
            var x = readCoordinate(buffer);
            var y = readCoordinate(buffer);
            var z = readCoordinate(buffer);
            return new Pos(x, y, z,
                readAngle(buffer.read(NetworkBuffer.SHORT)),
                readAngle(buffer.read(NetworkBuffer.SHORT)));
        }
    };

    /// The coordinates [#LP_POS] is going to decode from these, leaving the view alone.
    ///
    /// A recorder diffs one position against another, and a reader can only add up what it was
    /// actually sent, so the two disagree by whatever the rounding dropped. Diffing against this
    /// instead carries that remainder into the next delta rather than losing it, which holds a
    /// reader to half a step for the life of a chunk; measured over a day of recordings, diffing
    /// exact positions instead lets it reach forty times that before the next anchor.
    public static Pos quantizeCoordinates(Pos position) {
        return position.withCoord(
            quantizeCoordinate(position.x()),
            quantizeCoordinate(position.y()),
            quantizeCoordinate(position.z())
        );
    }

    private static double quantizeCoordinate(double value) {
        return Math.round(value * COORDINATE_SCALE) / COORDINATE_SCALE;
    }

    /// Zig-zag so that a delta of a few thousandths of a block, which is what most of these are,
    /// costs the same as a small positive number rather than the full width of a negative one.
    private static void writeCoordinate(NetworkBuffer buffer, double value) {
        var quantized = Math.round(value * COORDINATE_SCALE);
        buffer.write(NetworkBuffer.VAR_LONG, (quantized << 1) ^ (quantized >> 63));
    }

    private static double readCoordinate(NetworkBuffer buffer) {
        long value = buffer.read(NetworkBuffer.VAR_LONG);
        return ((value >>> 1) ^ -(value & 1)) / COORDINATE_SCALE;
    }

    /// The cast is the wrap, and the reason this is fixed point rather than a half float: a client
    /// never normalizes the yaw it sends, so a player who keeps turning reports one that grows
    /// without bound. An angle is a direction, so a turn too many costs nothing here, where a half
    /// float would be down to whole degrees by a few thousand and infinite past 65504.
    private static short writeAngle(float degrees) {
        return (short) Math.round(degrees * ANGLE_SCALE);
    }

    private static float readAngle(short value) {
        return (float) (value / ANGLE_SCALE);
    }
}
