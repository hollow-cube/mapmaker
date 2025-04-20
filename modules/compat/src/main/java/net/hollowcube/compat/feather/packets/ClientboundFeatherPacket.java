package net.hollowcube.compat.feather.packets;

import net.digitalingot.feather.serverapi.messaging.ClientMessageHandler;
import net.digitalingot.feather.serverapi.messaging.Message;
import net.digitalingot.feather.serverapi.messaging.MessageDecoder;
import net.digitalingot.feather.serverapi.messaging.MessageEncoder;
import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record ClientboundFeatherPacket(
        @NotNull Message<ClientMessageHandler> message
) implements ClientboundModPacket<ClientboundFeatherPacket> {
    public static final Type<ClientboundFeatherPacket> TYPE = Type.of(
            "feather",
            "client",
            NetworkBuffer.RAW_BYTES.transform(ClientboundFeatherPacket::new, ClientboundFeatherPacket::toBytes)
    );

    public ClientboundFeatherPacket(byte[] bytes) {
        this(MessageDecoder.CLIENT_BOUND.decode(bytes));
    }

    public byte[] toBytes() {
        return MessageEncoder.CLIENT_BOUND.encode(message);
    }

    @Override
    public Type<ClientboundFeatherPacket> getType() {
        return TYPE;
    }
}
