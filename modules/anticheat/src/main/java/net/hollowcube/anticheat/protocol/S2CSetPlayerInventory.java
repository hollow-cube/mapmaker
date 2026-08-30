package net.hollowcube.anticheat.protocol;

/// `play set_player_inventory`, keyed per inventory slot.
public sealed interface S2CSetPlayerInventory extends Packet permits S2CSetPlayerInventory.V776 {

    int slot();

    byte[] rest();

    record V776(int slot, byte[] rest) implements S2CSetPlayerInventory {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.remainingBytes());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(slot).bytes(rest);
        }
    }
}
