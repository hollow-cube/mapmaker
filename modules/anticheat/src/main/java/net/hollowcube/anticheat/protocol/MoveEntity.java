package net.hollowcube.anticheat.protocol;

/// The `play move_entity_*` family: relative moves in 1/4096 block units, applied by the client to
/// every entity it does not locally control.
///
/// The three variants carry different halves of the same state, so the half a variant does not
/// send reads as zero — [#hasPosition()] and [#hasRotation()] say which half is real, and a reader
/// that ignores them would apply a move the client never made.
public sealed interface MoveEntity extends EntityKeyed
    permits S2CMoveEntityPos, S2CMoveEntityPosRot, S2CMoveEntityRot, MoveEntity.Delta {

    boolean hasPosition();

    boolean hasRotation();

    default int deltaX() {
        return 0;
    }

    default int deltaY() {
        return 0;
    }

    default int deltaZ() {
        return 0;
    }

    default byte yRot() {
        return 0;
    }

    default byte xRot() {
        return 0;
    }

    boolean onGround();

    /// 26.3's moves, whose position is a [VecDelta] that may be a path of several steps.
    sealed interface Delta extends MoveEntity permits S2CMoveEntityPos.V777, S2CMoveEntityPosRot.V777 {

        VecDelta delta();

        @Override
        default int deltaX() {
            return delta().totalX();
        }

        @Override
        default int deltaY() {
            return delta().totalY();
        }

        @Override
        default int deltaZ() {
            return delta().totalZ();
        }
    }
}
