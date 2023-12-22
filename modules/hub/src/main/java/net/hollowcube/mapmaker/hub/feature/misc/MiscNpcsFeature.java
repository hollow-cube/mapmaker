package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.entity.BaseNpcEntity;
import net.hollowcube.mapmaker.hub.entity.NpcHandler;
import net.hollowcube.mapmaker.hub.entity.NpcPlayer;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import org.jetbrains.annotations.NotNull;

@AutoService(HubFeature.class)
@SuppressWarnings("UnstableApiUsage")
public class MiscNpcsFeature implements HubFeature {
    private static final Pos PLAYTIME_NPC_POS = new Pos(50, 37, 0.5, 90, 0);

    @Override
    public void init(@NotNull HubServer hub) {
        hub.instance().eventNode().addChild(NpcHandler.EVENT_NODE);

        var playtimeNpc = new NpcPlayer("Playtime Guy", PlayerSkin.fromUsername("notmattw"));
        playtimeNpc.setInstance(hub.instance(), PLAYTIME_NPC_POS).join();
        playtimeNpc.setHandler(this::handlePlaytimeClick);
    }

    private void handlePlaytimeClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand) {
        if (hand != Player.Hand.MAIN) return;

        var playerData = PlayerDataV2.fromPlayer(player);
        player.sendMessage("You have played for " + NumberUtil.formatPlayerPlaytime(playerData.totalPlaytime()));
    }

}
