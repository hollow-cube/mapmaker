package net.hollowcube.terraform.compat.axiom.packet;

import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;

/**
 * AxiomServerPacket is a {@link PluginMessagePacket} with strict types.
 * <p>
 * It allows the use of {@link net.minestom.server.entity.Player#sendPacket(SendablePacket)} which is convenient.
 */
public interface AxiomServerPacket {

}
