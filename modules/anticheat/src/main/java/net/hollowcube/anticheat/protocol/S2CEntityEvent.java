package net.hollowcube.anticheat.protocol;

/// `play entity_event`. Note the entity id is a plain int here, not a varint — the one packet in
/// the play table where it is. Events [#USE_ITEM_COMPLETE] and [#SWAP_HANDS] on the local player
/// change state no other packet carries (the item-use sprint gate; the held items), so those two
/// are fenced for self.
public sealed interface S2CEntityEvent extends EntityKeyed permits S2CEntityEvent.V776 {

    /// `EntityEvent.USE_ITEM_COMPLETE` — ends `isUsingItem`, which gates sprinting.
    byte USE_ITEM_COMPLETE = 9;
    /// `EntityEvent.SWAP_HANDS` — a real `setItemSlot` on both hands.
    byte SWAP_HANDS = 55;

    byte event();

    record V776(int entityId, byte event) implements S2CEntityEvent {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.i32(), reader.i8());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.i32(entityId).u8(event);
        }
    }
}
