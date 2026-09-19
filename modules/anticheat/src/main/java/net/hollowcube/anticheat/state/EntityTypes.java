package net.hollowcube.anticheat.state;

/// The `minecraft:entity_type` ids the capture has to recognise, per protocol version.
///
/// `minecraft:entity_type` is not a datapack registry, so an id is simply the position of the
/// `register(...)` call in `EntityTypes` in that version's decompile. 776 was checked against the
/// `minecraft:entity_type` tag payloads in the checked-in 776 capture fixtures (`minecraft:zombies`,
/// `minecraft:skeletons` and `minecraft:boat` all resolve to exactly their families); 777 against
/// Minestom's 26.3 registry data. 26.3 inserted `cushion`, `poplar_boat` and `poplar_chest_boat`.
public record EntityTypes(int blockDisplay, int interaction, int itemDisplay, int textDisplay, int player) {

    public static final EntityTypes V776 = new EntityTypes(15, 69, 72, 132, 156);
    public static final EntityTypes V777 = new EntityTypes(15, 70, 73, 135, 159);

    /// The three types that override none of `isPickable`, `isPushable` or `canBeCollidedWith`, so
    /// they can neither be hit, push, nor collide, and are dropped from the capture. `Interaction`
    /// is pickable and is deliberately not in this set.
    public boolean isDisplay(int entityTypeId) {
        return entityTypeId == blockDisplay || entityTypeId == itemDisplay || entityTypeId == textDisplay;
    }
}
