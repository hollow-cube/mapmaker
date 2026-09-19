package net.hollowcube.anticheat.protocol;

import java.util.ArrayList;
import java.util.List;

/// `VecDelta`, the relative move 26.3's `move_entity_pos(_rot)` carries in 1/4096 block units:
/// either one delta, or a path of steps each relative to the one before it and landing `ticks`
/// into the interpolation.
///
/// The step count travels in the packet's properties varint, above the on-ground flag in bit 0, so
/// the varint is read by the packet and written here together with the move.
public sealed interface VecDelta {

    int stepCount();

    /// The move from where the entity was to where the last step leaves it. Summed, so a stepped
    /// path can exceed the `short` range a single delta is limited to.
    int totalX();

    int totalY();

    int totalZ();

    default void encode(ByteWriter writer, boolean onGround) {
        writer.varInt((onGround ? 1 : 0) | stepCount() << 1);
        switch (this) {
            case Linear(short x, short y, short z) -> writer.i16(x).i16(y).i16(z);
            case Stepped(var steps) -> {
                for (var step : steps) writer.varInt(step.ticks).i16(step.x).i16(step.y).i16(step.z);
            }
        }
    }

    static boolean onGround(int properties) {
        return (properties & 1) != 0;
    }

    /// `VecDelta#read`: a positive step count is a stepped path, anything else one linear delta.
    static VecDelta decode(ByteReader reader, int properties) {
        int stepCount = properties >>> 1;
        if (stepCount <= 0) return new Linear(reader.i16(), reader.i16(), reader.i16());
        // The same bound the client applies: a step is at least a one-byte varint and three shorts.
        if (stepCount > reader.remaining() / 7)
            throw new ProtocolException("VecDelta with " + stepCount + " steps in " + reader.remaining() + " bytes");
        var steps = new ArrayList<Step>(stepCount);
        for (int i = 0; i < stepCount; i++)
            steps.add(new Step(reader.varInt(), reader.i16(), reader.i16(), reader.i16()));
        return new Stepped(List.copyOf(steps));
    }

    record Linear(short x, short y, short z) implements VecDelta {

        @Override
        public int stepCount() {
            return 0;
        }

        @Override
        public int totalX() {
            return x;
        }

        @Override
        public int totalY() {
            return y;
        }

        @Override
        public int totalZ() {
            return z;
        }
    }

    record Stepped(List<Step> steps) implements VecDelta {

        @Override
        public int stepCount() {
            return steps.size();
        }

        @Override
        public int totalX() {
            int total = 0;
            for (var step : steps) total += step.x;
            return total;
        }

        @Override
        public int totalY() {
            int total = 0;
            for (var step : steps) total += step.y;
            return total;
        }

        @Override
        public int totalZ() {
            int total = 0;
            for (var step : steps) total += step.z;
            return total;
        }
    }

    record Step(int ticks, short x, short y, short z) {}
}
