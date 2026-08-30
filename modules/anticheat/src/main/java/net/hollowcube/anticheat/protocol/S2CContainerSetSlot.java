package net.hollowcube.anticheat.protocol;

/// `play container_set_slot`, keyed per container.
public sealed interface S2CContainerSetSlot extends ContainerKeyed permits S2CContainerSetSlot.V776 {

    /// `varint stateId, short slot, item`, kept as bytes: the item is never decoded.
    byte[] rest();

    record V776(int containerId, byte[] rest) implements S2CContainerSetSlot {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.remainingBytes());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(containerId).bytes(rest);
        }
    }
}
