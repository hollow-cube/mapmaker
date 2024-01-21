package net.hollowcube.map.entity.potion;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityPotionAddEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public interface PotionHandler {

    void apply(@NotNull Player player, int level);

    void remove(@NotNull Player player);

    @NotNull EventNode<InstanceEvent> EVENT_NODE = EventNode.type("potion-handler", EventFilter.INSTANCE)
            .addListener(EntityPotionAddEvent.class, event -> {
                if (!(event.getEntity() instanceof Player player)) return;

                var potion = event.getPotion();
                var effect = PotionInfo.getByVanillaEffect(potion.effect());
                if (effect == null || effect.handler() == null) return;

                effect.handler().apply(player, potion.amplifier());
            });

}
