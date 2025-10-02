package net.hollowcube.compat.vanilla;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.vanilla.packets.ServerboundBrandPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

@AutoService(CompatProvider.class)
public class VanillaCompatProvider implements CompatProvider {

    private static final Tag<@NotNull String> CLIENT_BRAND = Tag.<String>Transient("packets:client/brand").defaultValue("");

    @Override
    public void registerPackets(PacketRegistry registry) {
        registry.register(ServerboundBrandPacket.TYPE, (player, packet) ->
            player.setTag(CLIENT_BRAND, packet.brand())
        );
    }
}
