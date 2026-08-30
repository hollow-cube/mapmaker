package net.hollowcube.anticheat.protocol;

/// The `play move_entity_*` family: relative moves in 1/4096 block units, applied by the client to
/// every entity it does not locally control.
///
/// The three variants carry different halves of the same state, so the half a variant does not
/// send reads as zero — [#hasPosition()] and [#hasRotation()] say which half is real, and a reader
/// that ignores them would apply a move the client never made.
public sealed interface MoveEntity extends EntityKeyed
    permits S2CMoveEntityPos, S2CMoveEntityPosRot, S2CMoveEntityRot {

    boolean hasPosition();

    boolean hasRotation();

    default short deltaX() {
        return 0;
    }

    default short deltaY() {
        return 0;
    }

    default short deltaZ() {
        return 0;
    }

    default byte yRot() {
        return 0;
    }

    default byte xRot() {
        return 0;
    }

    boolean onGround();
}
