package net.hollowcube.mapmaker.cosmetic.impl.accessory;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.cosmetic.impl.ModelCosmeticImpl;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractAccessoryImpl extends ModelCosmeticImpl {
    public static void addListeners(@NotNull EventNode<Event> eventNode) {
        eventNode.addListener(PlayerUseItemEvent.class, event -> {
            var player = event.getPlayer();
            if (event.getHand() != PlayerHand.OFF || !player.getItemInMainHand().isAir())
                return; // TODO: using air check here is bad, but oh well.

            var playerData = PlayerDataV2.fromPlayer(player);
            var accessory = Cosmetic.byId(CosmeticType.ACCESSORY, playerData.getSetting(CosmeticType.ACCESSORY.setting()).id());
            if (accessory != null && accessory.impl() instanceof AbstractAccessoryImpl impl) {
                impl.useItem(player);
            }
        });

    }

    public AbstractAccessoryImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
    }

    public abstract void useItem(@NotNull Player player);
}
