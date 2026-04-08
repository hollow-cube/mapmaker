package net.hollowcube.compat.blacklisted;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.ModChannelRegisterEvent;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;

import java.util.ArrayList;
import java.util.List;

@AutoService(CompatProvider.class)
public class BlacklistedModsProvider implements CompatProvider {

    private static final Component KICK_MESSAGE = Component.text(
        "You've been kicked for using mod(s) which are disallowed on the server."
    ).color(NamedTextColor.RED);

    @Override
    public void registerPackets(PacketRegistry registry) {
        registry.registerNamespaceHandler("jumpoverfences", (player, _) ->
            disconnectWithMods(player, List.of(Component.text("Jump Over Fences")))
        );
    }

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(ModChannelRegisterEvent.class, event -> {
            var player = event.getPlayer();
            var channels = event.getChannels();
            var bannedMods = new ArrayList<Component>();
            if (channels.contains("servux:tweaks")) bannedMods.add(Component.text("Tweakeroo"));

            disconnectWithMods(player, bannedMods);
        });
    }

    private static void disconnectWithMods(Player player, List<Component> mods) {
        if (mods.isEmpty()) return;
        var text = Component.text(
            "\nPlease remove the following mod(s) to join:\n"
        ).color(NamedTextColor.RED);
        var size = mods.size();
        var counter = 0;
        for (Component mod : mods) {
            text = text.append(mod.color(NamedTextColor.GRAY));
            if (++counter != size) {
                text = text.append(Component.text(", "));
            }
        }
        PlayerUtil.disconnect(player, KICK_MESSAGE.append(text));
    }
}
