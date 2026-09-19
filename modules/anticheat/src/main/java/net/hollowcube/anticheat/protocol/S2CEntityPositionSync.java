package net.hollowcube.anticheat.protocol;

/// `play entity_position_sync`: an absolute position for a tracked entity, and for the player's
/// vehicle a move of the player with it.
public sealed interface S2CEntityPositionSync extends EntityKeyed
    permits S2CEntityPositionSync.V776, S2CEntityPositionSync.V777 {

    double x();

    double y();

    double z();

    float yRot();

    float xRot();

    boolean onGround();

    /// A sync placing an entity at rest, which is the only kind the capture synthesizes.
    @FunctionalInterface
    interface AtRest {
        S2CEntityPositionSync create(
            int entityId, double x, double y, double z, float yRot, float xRot, boolean onGround);
    }

    record V776(int entityId, PositionMoveRotation values, boolean onGround)
        implements S2CEntityPositionSync {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), PositionMoveRotation.decode(reader), reader.bool());
        }

        public static V776 atRest(int entityId, double x, double y, double z, float yRot, float xRot, boolean onGround) {
            return new V776(entityId, new PositionMoveRotation(x, y, z, 0, 0, 0, yRot, xRot), onGround);
        }

        @Override
        public double x() {
            return values.x();
        }

        @Override
        public double y() {
            return values.y();
        }

        @Override
        public double z() {
            return values.z();
        }

        @Override
        public float yRot() {
            return values.yRot();
        }

        @Override
        public float xRot() {
            return values.xRot();
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId);
            values.encode(writer);
            writer.bool(onGround);
        }
    }

    /// The delta movement is gone and the position became a [PositionPath]; the entity ends up at
    /// the path's end either way.
    record V777(int entityId, PositionPath path, float yRot, float xRot, boolean onGround)
        implements S2CEntityPositionSync {

        public static V777 decode(ByteReader reader) {
            return new V777(reader.varInt(), PositionPath.decode(reader), reader.f32(), reader.f32(), reader.bool());
        }

        public static V777 atRest(int entityId, double x, double y, double z, float yRot, float xRot, boolean onGround) {
            return new V777(entityId, new PositionPath.Linear(x, y, z), yRot, xRot, onGround);
        }

        @Override
        public double x() {
            return path.endX();
        }

        @Override
        public double y() {
            return path.endY();
        }

        @Override
        public double z() {
            return path.endZ();
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(entityId);
            path.encode(writer);
            writer.f32(yRot).f32(xRot).bool(onGround);
        }
    }
}
