package net.hollowcube.compat.blacklisted;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.ModChannelRegisterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.event.GlobalEventHandler;

import java.util.ArrayList;

@AutoService(CompatProvider.class)
public class BlacklistedModsProvider implements CompatProvider {

    private static final Component KICK_MESSAGE = Component.text(
            "You've been kicked for using mod(s) which are disallowed on the server.\nPlease remove them to join."
    ).color(NamedTextColor.RED);

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(ModChannelRegisterEvent.class, event -> {
            var player = event.getPlayer();
            var channels = event.getChannels();
            var bannedMods = new ArrayList<Component>();
            if (channels.contains("servux:tweaks")) {
                bannedMods.add(Component.text("Servux Tweaks"));
            }

            if (!bannedMods.isEmpty()) {
                PlayerUtil.disconnect(player, KICK_MESSAGE);
            }
        });
    }
}
