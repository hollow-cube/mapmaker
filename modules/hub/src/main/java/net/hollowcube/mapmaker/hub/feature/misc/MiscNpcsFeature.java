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

        var skin = new PlayerSkin(
                "ewogICJ0aW1lc3RhbXAiIDogMTcwNDY4NjAwMTk5NSwKICAicHJvZmlsZUlkIiA6ICJhY2ViMzI2ZmRhMTU0NWJjYmYyZjExOTQwYzIxNzgwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJub3RtYXR0dyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kZDBiZDllMGIxNTRiMjkwOWIwNmJiMzIyMjVkOWMwYTczYTQ0NWRiMzYxOTU1OGNlZGQ4OTI0ZjljOTE1YjM3IgogICAgfSwKICAgICJDQVBFIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yMzQwYzBlMDNkZDI0YTExYjE1YThiMzNjMmE3ZTllMzJhYmIyMDUxYjI0ODFkMGJhN2RlZmQ2MzVjYTdhOTMzIgogICAgfQogIH0KfQ==",
                "ZjmpLOhUt7F9feBefYcRowbwTSZZmGsX1otJRqywNrZNQ1ZQ7MR0WpKzin38LA+UTBF7ohqeoINfMiQqFR9X48adDPU8570qY4bJdYjmMImunKKNaQ/jLsDNDpMnZdj8ib3ELxNJjs8fVc8CVG6QjHbmNJSjAjAMopvfs5S7+RT5gu91wq16IWZjUpTQJxT27VWvc22YET5J+wKeI4P827TyumAc9sTpSkVB+vkthh2T3e2F8eNwMfgx59fE0wTGInwYWlIle5tZ7CDw4Ok3L30OpOUNC5wmcDpiqiIVUwZWdaR9vAyvqUdDDQdqy77h4jSiLpRIPh0rD3XNMySPxZoOjFajCTF+64cT9aaqxv93v6J3P699HTupEwIu8CIdsYFPXggGdPNiiYY0H1u0P+FY1/AS9uIbRgC/1L/ed6Cma6yQ5dBzDi45N/aqzKi8DZrG0Wgw00mzsq+uTB2OB1/+coWWvBCMNaoya+QuVLvFyT5tu0oMsQcB2LBtInhx/dYoB39um8FhBdEF/zNzSEZ/HIGmxL7HO6PVl/fpv+9CbbW+3Q0g1xwBt7p8RBL+Bjj9j+OkUTyuzLTq/Bjw1sgxYBVFk5kHIhCb8onys7r8IxV2R4vmofTTFCSaR40IqNbdACmsDeKx91LUC4IJS91HH5pIqprR2NLX8QNLKzA="
        );
        var playtimeNpc = new NpcPlayer("Playtime Guy", skin);
        playtimeNpc.setInstance(hub.instance(), PLAYTIME_NPC_POS).join();
        playtimeNpc.setHandler(this::handlePlaytimeClick);
    }

    private void handlePlaytimeClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand) {
        if (hand != Player.Hand.MAIN) return;

        var playerData = PlayerDataV2.fromPlayer(player);
        player.sendMessage("You have played for " + NumberUtil.formatPlayerPlaytime(playerData.totalPlaytime()));
    }

}
