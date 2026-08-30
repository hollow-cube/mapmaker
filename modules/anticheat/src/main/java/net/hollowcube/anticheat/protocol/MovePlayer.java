package net.hollowcube.anticheat.protocol;

/// The C2S `move_player_*` family, the core movement input.
///
/// 26.2 packs the trailing status byte as `onGround | horizontalCollision << 1`
/// (`ServerboundMovePlayerPacket#packFlags`); older versions sent a bare `onGround` boolean, which
/// happens to be the same byte for the collision-free case but is a different field.
///
/// As with [MoveEntity], a variant that sends no position or no rotation reads as zero for it, so
/// [#hasPosition()] and [#hasRotation()] are what say whether the values mean anything.
public sealed interface MovePlayer extends Packet
    permits C2SMovePlayerPos, C2SMovePlayerPosRot, C2SMovePlayerRot, C2SMovePlayerStatusOnly {

    int FLAG_ON_GROUND = 1;
    int FLAG_HORIZONTAL_COLLISION = 2;

    boolean hasPosition();

    boolean hasRotation();

    default double x() {
        return 0;
    }

    default double y() {
        return 0;
    }

    default double z() {
        return 0;
    }

    default float yRot() {
        return 0;
    }

    default float xRot() {
        return 0;
    }

    int flags();

    default boolean onGround() {
        return (flags() & FLAG_ON_GROUND) != 0;
    }

    default boolean horizontalCollision() {
        return (flags() & FLAG_HORIZONTAL_COLLISION) != 0;
    }
}
