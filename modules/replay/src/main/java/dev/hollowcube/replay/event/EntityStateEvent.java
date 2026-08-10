package dev.hollowcube.replay.event;

import net.minestom.server.entity.EntityPose;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

/// The pose and metadata flags an entity is currently in.
///
/// These always change together and are always read together, so they travel as one event with the
/// flags packed into a byte rather than as one event each that says almost nothing.
///
/// The bits are vanilla's shared entity flags byte, so that what is carried here is exactly what a
/// client can see. Riptide is the exception: it lives in the living entity flags rather than the
/// shared byte, and takes the bit vanilla leaves unused.
public record EntityStateEvent(
    int entityId,
    EntityPose pose,
    boolean onFire,
    boolean sneaking,
    boolean riptideSpinAttack,
    boolean sprinting,
    boolean swimming,
    boolean invisible,
    boolean glowing,
    boolean flyingWithElytra
) implements ReplayEvent {
    private static final int ON_FIRE = 0x01;
    private static final int SNEAKING = 0x02;
    private static final int RIPTIDE_SPIN_ATTACK = 0x04;
    private static final int SPRINTING = 0x08;
    private static final int SWIMMING = 0x10;
    private static final int INVISIBLE = 0x20;
    private static final int GLOWING = 0x40;
    private static final int FLYING_WITH_ELYTRA = 0x80;

    public static final NetworkBuffer.Type<EntityStateEvent> NETWORK_TYPE = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, EntityStateEvent::entityId,
        NetworkBuffer.POSE, EntityStateEvent::pose,
        NetworkBuffer.BYTE, EntityStateEvent::flags,
        EntityStateEvent::new
    );

    public EntityStateEvent(int entityId, EntityPose pose, byte flags) {
        this(
            entityId, pose,
            (flags & ON_FIRE) != 0,
            (flags & SNEAKING) != 0,
            (flags & RIPTIDE_SPIN_ATTACK) != 0,
            (flags & SPRINTING) != 0,
            (flags & SWIMMING) != 0,
            (flags & INVISIBLE) != 0,
            (flags & GLOWING) != 0,
            (flags & FLYING_WITH_ELYTRA) != 0
        );
    }

    public byte flags() {
        var flags = 0;
        if (onFire) flags |= ON_FIRE;
        if (sneaking) flags |= SNEAKING;
        if (riptideSpinAttack) flags |= RIPTIDE_SPIN_ATTACK;
        if (sprinting) flags |= SPRINTING;
        if (swimming) flags |= SWIMMING;
        if (invisible) flags |= INVISIBLE;
        if (glowing) flags |= GLOWING;
        if (flyingWithElytra) flags |= FLYING_WITH_ELYTRA;
        return (byte) flags;
    }
}
