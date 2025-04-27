package net.hollowcube.compat.laby;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.discord.DiscordRichPresenceProvider;
import net.labymod.serverapi.core.model.feature.DiscordRPC;
import net.labymod.serverapi.server.minestom.LabyModProtocolService;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;

@AutoService({CompatProvider.class, DiscordRichPresenceProvider.class})
public class LabyCompatProvider implements CompatProvider, DiscordRichPresenceProvider {

    @Override
    public void registerListeners(GlobalEventHandler events) {
        // todo this probably adds a load of junk we don't need. should probably just implement the packets we need ourselves.
        //   however I am lazy so for now, this will do.
        LabyModProtocolService.initialize();
    }

    @Override
    public void setRichPresence(Player player, String playerState, String gameName, String gameVariantName) {
        var labyModPlayer = LabyModProtocolService.get().getPlayer(player.getUuid());
        if (labyModPlayer == null) {
            return;
        }

        // labymod sucks and we can only control one little bit
        final var details = playerState + " " + gameName + (gameVariantName != null && !gameVariantName.isEmpty() ? " (" + gameVariantName + ")" : "");
        labyModPlayer.sendDiscordRPC(DiscordRPC.create(details));
    }

    @Override
    public void clearRichPresence(Player player) {
        var labyModPlayer = LabyModProtocolService.get().getPlayer(player.getUuid());
        if (labyModPlayer == null) {
            return;
        }
        labyModPlayer.sendDiscordRPC(DiscordRPC.createReset());
    }

    @Override
    public boolean isRichPresenceSupportedFor(Player player) {
        return LabyModProtocolService.get().isUsingLabyMod(player.getUuid());
    }


}
