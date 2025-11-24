package net.hollowcube.mapmaker.hub.feature.event.christmas;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.event.EventData;
import net.hollowcube.mapmaker.hub.util.HubTime;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

public class PresentObjectHandler extends ObjectEntityHandler {

    public static final String ID = "hub:present";

    private final NpcItemModel model;

    private int day = -1;

    public PresentObjectHandler(InteractionEntity entity) {
        super(ID, entity);

        this.model = new NpcItemModel();
        this.model.setAutoViewable(false);
        this.model.setInstance(entity.getInstance(), entity.getPosition().add(0, 0.5, 0).withYaw(45));

        this.onDataChange(null);
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        this.day = this.data().getInt("day", -1);
        this.model.setModel(PresentTextures.getForDay(this.day));
    }

    @Override
    public boolean canSendToPlayer(Player player) {
        if (this.day <= 0) return false;
        return HubTime.now().getDayOfMonth() >= this.day;
    }

    @Override
    public void addViewer(MapWorld world, Player player) {
        if (!this.canSendToPlayer(player)) return;
        this.model.addViewer(player);
    }

    @Override
    public void onPlayerInteract(Player player) {
        if (!canSendToPlayer(player)) return; // Sanity check
        var world = MapWorld.forPlayer(player);
        var playerData = PlayerData.fromPlayer(player);
        var eventData = playerData.getSetting(EventData.SETTING);

        if (world == null) return;
        if (eventData.hasPresent(day)) {
            player.sendMessage("You have already collected this present!");
        } else {
            playerData.setSetting(EventData.SETTING, eventData.withPresent(day));
            FutureUtil.submitVirtual(() -> playerData.writeUpdatesUpstream(world.server().playerService()));

            player.sendMessage("You have collected your present for day " + day + "! Merry Christmas!");
        }
    }
}
