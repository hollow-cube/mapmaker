package net.hollowcube.enbs;

import net.minestom.server.network.NetworkBuffer;

import java.util.Map;

final class ENBSTypes {
    public static final NetworkBuffer.Type<Byte> BYTE = NetworkBuffer.BYTE;
    public static final NetworkBuffer.Type<Integer> INT = NetworkBuffer.VAR_INT;
    public static final NetworkBuffer.Type<String> STRING = NetworkBuffer.STRING;
    public static final NetworkBuffer.Type<Map<String, String>> METADATA = NetworkBuffer.STRING.mapValue(NetworkBuffer.STRING);
}
