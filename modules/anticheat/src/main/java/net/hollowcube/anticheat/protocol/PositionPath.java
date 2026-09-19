package net.hollowcube.anticheat.protocol;

import java.util.ArrayList;
import java.util.List;

/// `PositionPath`, the absolute position 26.3's `entity_position_sync` carries: a single end
/// position, or the steps an entity interpolates through, the last of which is where it ends up.
public sealed interface PositionPath {

    int LINEAR = 0;
    int STEPPED = 1;

    double endX();

    double endY();

    double endZ();

    void encode(ByteWriter writer);

    /// The type is a varint through `ByIdMap.continuous(ZERO)`, which reads an id it does not know
    /// as linear. Such a packet cannot re-encode to the bytes it came from, so it is refused
    /// instead: the frame is still kept, only the model goes without it.
    static PositionPath decode(ByteReader reader) {
        int type = reader.varInt();
        return switch (type) {
            case LINEAR -> new Linear(reader.f64(), reader.f64(), reader.f64());
            case STEPPED -> {
                int count = reader.varInt();
                // `Stepped` takes its end position from the last step, so an empty path is one the
                // client itself fails to decode.
                if (count <= 0 || count > reader.remaining() / 25)
                    throw new ProtocolException("bad stepped path length: " + count);
                var steps = new ArrayList<Step>(count);
                for (int i = 0; i < count; i++)
                    steps.add(new Step(reader.f64(), reader.f64(), reader.f64(), reader.varInt()));
                yield new Stepped(List.copyOf(steps));
            }
            default -> throw new ProtocolException("unknown position path type: " + type);
        };
    }

    record Linear(double endX, double endY, double endZ) implements PositionPath {

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(LINEAR).f64(endX).f64(endY).f64(endZ);
        }
    }

    record Stepped(List<Step> steps) implements PositionPath {

        @Override
        public double endX() {
            return steps.getLast().x;
        }

        @Override
        public double endY() {
            return steps.getLast().y;
        }

        @Override
        public double endZ() {
            return steps.getLast().z;
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(STEPPED).varInt(steps.size());
            for (var step : steps) writer.f64(step.x).f64(step.y).f64(step.z).varInt(step.tickOffset);
        }
    }

    /// `PositionStep`.
    record Step(double x, double y, double z, int tickOffset) {}
}
