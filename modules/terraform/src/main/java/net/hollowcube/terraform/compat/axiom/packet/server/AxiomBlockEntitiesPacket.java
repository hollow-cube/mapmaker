package net.hollowcube.terraform.compat.axiom.packet.server;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.minestom.server.network.NetworkBuffer.STRING;

@SuppressWarnings("UnstableApiUsage")
public record AxiomBlockEntitiesPacket(
        List<String> blockEntities //todo string is wrong, just not sure the type yet and we always send empty
) implements AxiomServerPacket {

    @Override
    public @NotNull String packetChannel() {
        return "axiom:block_entities";
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        buffer.writeCollection(blockEntities, (b, s) -> b.write(STRING, s));
    }

}
