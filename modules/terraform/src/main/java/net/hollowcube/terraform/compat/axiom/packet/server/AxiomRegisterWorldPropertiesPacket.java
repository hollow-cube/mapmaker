package net.hollowcube.terraform.compat.axiom.packet.server;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public record AxiomRegisterWorldPropertiesPacket(
        @NotNull Map<Category, List<WorldProperty>> properties
) implements AxiomServerPacket {

    public record Category(
            @NotNull String name,
            boolean localizeName
    ) {
        public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
            buffer.write(STRING, name);
            buffer.write(BOOLEAN, localizeName);
        }
    }

    public record WorldProperty(
            @NotNull NamespaceID id,
            @NotNull String name,
            boolean localizeName,
            int typeId,
            byte[] properties,
            byte[] value
    ) {
        public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
            buffer.write(STRING, id.asString());
            buffer.write(STRING, name);
            buffer.write(BOOLEAN, localizeName);
            buffer.write(VAR_INT, typeId);
            buffer.write(RAW_BYTES, properties);
            buffer.write(BYTE_ARRAY, value);
        }
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        buffer.write(VAR_INT, properties.size());
        for (var entry : properties.entrySet()) {
            entry.getKey().write(buffer, apiVersion);
            buffer.writeCollection(entry.getValue(), (b, property) -> property.write(b, apiVersion));
        }
    }

    @Override
    public @NotNull String packetChannel() {
        return "axiom:register_world_properties";
    }

}
