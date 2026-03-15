package net.hollowcube.mapmaker.cosmetic.impl.accessory;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.cosmetic.impl.ModelCosmeticImpl;
import net.hollowcube.mapmaker.player.PlayerData;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;

public abstract class AbstractAccessoryImpl extends ModelCosmeticImpl {
    public static void addListeners(EventNode<Event> eventNode) {
        eventNode.addListener(PlayerUseItemEvent.class, event -> {
            var player = event.getPlayer();
            if (event.getHand() != PlayerHand.OFF || !player.getItemInMainHand().isAir())
                return; // TODO: using air check here is bad, but oh well.

            var playerData = PlayerData.fromPlayer(player);
            var accessory = Cosmetic.byId(CosmeticType.ACCESSORY, playerData.getSetting(CosmeticType.ACCESSORY.setting()));
            if (accessory != null && accessory.impl() instanceof AbstractAccessoryImpl impl) {
                impl.useItem(player);
            }
        });

    }

    public AbstractAccessoryImpl(Cosmetic cosmetic) {
        super(cosmetic);
    }

    public abstract void useItem(Player player);
}
