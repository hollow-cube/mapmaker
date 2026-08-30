package net.hollowcube.anticheat.protocol;

/// `play container_set_content`, which resets every slot of its container.
public sealed interface S2CContainerSetContent extends ContainerKeyed permits S2CContainerSetContent.V776 {

    /// Everything after the container id, kept as bytes: splitting it means decoding item stacks.
    byte[] rest();

    record V776(int containerId, byte[] rest) implements S2CContainerSetContent {

        public static V776 decode(ByteReader reader) {
            return new V776(reader.varInt(), reader.remainingBytes());
        }

        @Override
        public void encode(ByteWriter writer) {
            writer.varInt(containerId).bytes(rest);
        }
    }
}
