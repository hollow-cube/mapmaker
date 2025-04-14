package net.hollowcube.compat.feather.packets;

import net.digitalingot.feather.serverapi.messaging.Message;
import net.digitalingot.feather.serverapi.messaging.MessageDecoder;
import net.digitalingot.feather.serverapi.messaging.MessageEncoder;
import net.digitalingot.feather.serverapi.messaging.ServerMessageHandler;
import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.minestom.server.network.NetworkBuffer;

public record ServerboundFeatherPacket(
        Message<ServerMessageHandler> message
) implements ServerboundModPacket<ServerboundFeatherPacket> {
    public static final Type<ServerboundFeatherPacket> TYPE = Type.of(
            "feather",
            "client",
            NetworkBuffer.RAW_BYTES.transform(ServerboundFeatherPacket::new, ServerboundFeatherPacket::toBytes)
    );

    public ServerboundFeatherPacket(byte[] bytes) {
        this(MessageDecoder.SERVER_BOUND.decode(bytes));
    }

    public byte[] toBytes() {
        return MessageEncoder.SERVER_BOUND.encode(message);
    }

    @Override
    public Type<ServerboundFeatherPacket> getType() {
        return TYPE;
    }
}
