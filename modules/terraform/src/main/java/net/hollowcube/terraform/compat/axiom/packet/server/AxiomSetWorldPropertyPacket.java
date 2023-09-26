package net.hollowcube.terraform.compat.axiom.packet.server;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public record AxiomSetWorldPropertyPacket(
        @NotNull NamespaceID id,
        int typeId,
        byte[] value
) implements AxiomServerPacket {

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        buffer.write(STRING, id.asString());
        buffer.write(VAR_INT, typeId);
        buffer.write(BYTE_ARRAY, value);
    }

    @Override
    public @NotNull String packetChannel() {
        return "axiom:set_world_property";
    }

}
