package net.hollowcube.compat.lunar.packets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.lunar.payload.LunarPayload;
import net.hollowcube.compat.lunar.payload.LunarPayloadType;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.ThrowingFunction;

public record ServerboundLunarPacket(LunarPayload payload) implements ServerboundModPacket<ServerboundLunarPacket> {

    private static final ThrowingFunction<NetworkBuffer, ServerboundLunarPacket> READER = buffer -> {
        var string = new String(buffer.read(NetworkBuffer.RAW_BYTES));
        var json = JsonParser.parseString(string);
        if (json instanceof JsonObject object) {
            var type = object.get("@type").getAsString();
            var payloadType = LunarPayloadType.REGISTRY.get(type);
            if (payloadType != null) {
                return new ServerboundLunarPacket(payloadType.codec().decode(Transcoder.JSON, object).orElseThrow());
            }
        }
        return new ServerboundLunarPacket(new LunarPayload.Unhandled(json));
    };

    public static final Type<ServerboundLunarPacket> APOLLO_JSON_TYPE = Type.of("apollo", "json", READER);

    @Override
    public Type<ServerboundLunarPacket> getType() {
        return null;
    }
}
