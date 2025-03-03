package net.hollowcube.compat.axiom.data.buffers;

import net.minestom.server.network.NetworkBuffer;

public record AxiomBiomeBuffer() implements AxiomBuffer {

    public static AxiomBiomeBuffer read(NetworkBuffer ignored) {
        return new AxiomBiomeBuffer();
    }
}
