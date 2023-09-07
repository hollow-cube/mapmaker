package net.hollowcube.terraform.compat.axiom.packet.server;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public record AxiomEnablePacket(
        boolean enabled,
        //todo world properties
        int maxBufferSize,
        boolean sendSourceInfo,
        boolean sendSourceSettings,
        int maxReachDistance,
        int maxViews,
        boolean editableViews
) implements AxiomServerPacket {

    public AxiomEnablePacket(boolean enabled) {
        this(enabled, 0, false, false, 0, 0, false);
        Check.argCondition(enabled, "all settings must be provided for enable packet");
    }

    @Override
    public @NotNull String packetChannel() {
        return "axiom:enable";
    }

    @Override
    public void write(@NotNull NetworkBuffer buffer, int apiVersion) {
        buffer.write(BOOLEAN, enabled);
        if (enabled) {
            buffer.write(BYTE, (byte) 0);
            buffer.write(INT, maxBufferSize);
            buffer.write(BOOLEAN, sendSourceInfo);
            buffer.write(BOOLEAN, sendSourceSettings);
            buffer.write(VAR_INT, maxReachDistance);
            buffer.write(VAR_INT, maxViews);
            buffer.write(BOOLEAN, editableViews);
        }
    }
}
