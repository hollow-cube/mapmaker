package net.hollowcube.terraform.compat.axiom.packet.server;

import net.hollowcube.terraform.compat.axiom.packet.AxiomServerPacket;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public record AxiomSetWorldPropertyPacket(
        @NotNull NamespaceID id,
        int typeId,
        byte[] value
) implements AxiomServerPacket {

//    @Override
//    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
//        buffer.write(STRING, id.asString());
//        buffer.write(VAR_INT, typeId);
//        buffer.write(BYTE_ARRAY, value);
//    }

}
