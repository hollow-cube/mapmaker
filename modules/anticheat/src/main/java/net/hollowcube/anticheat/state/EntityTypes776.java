package net.hollowcube.anticheat.state;

/// The `minecraft:entity_type` ids the capture has to recognise, for protocol version 776 (26.2).
///
/// `minecraft:entity_type` is not a datapack registry, so an id is simply the position of the
/// `register(...)` call in `EntityTypes` in the 26.2 decompile — 158 of them, `player` and
/// `fishing_bobber` last. The four below were read off that order and checked against the
/// `minecraft:entity_type` tag payloads in the checked-in 776 capture fixtures (`minecraft:zombies`,
/// `minecraft:skeletons` and `minecraft:boat` all resolve to exactly their families).
public final class EntityTypes776 {

    // TODO generate from registry data
    public static final int BLOCK_DISPLAY = 15;
    public static final int INTERACTION = 69;
    public static final int ITEM_DISPLAY = 72;
    public static final int TEXT_DISPLAY = 132;
    public static final int PLAYER = 156;

    /// The three types that override none of `isPickable`, `isPushable` or `canBeCollidedWith`, so
    /// they can neither be hit, push, nor collide, and are dropped from the capture. `Interaction`
    /// is pickable and is deliberately not in this set.
    public static boolean isDisplay(int entityTypeId) {
        return entityTypeId == BLOCK_DISPLAY || entityTypeId == ITEM_DISPLAY || entityTypeId == TEXT_DISPLAY;
    }

    private EntityTypes776() {}
}
