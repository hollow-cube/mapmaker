package net.hollowcube.mapmaker.map.feature.play.vanilla;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.util.PlayerRiptideExtension;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPreEatEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class TridentFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:vanilla/trident", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::handleInitPlayer)
            .addListener(PlayerPreEatEvent.class, this::handleBeginTridentThrow);
    // TODO: Fix from 1.21.3 update
//            .addListener(ItemUpdateStateEvent.class, this::handleTridentThrow);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;
        if (world.map().settings().getVariant() != MapVariant.PARKOUR)
            return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    private void handleInitPlayer(@NotNull MapPlayerInitEvent event) {
        if ((event.getPlayer() instanceof PlayerRiptideExtension p))
            p.cancelRiptideAttack();
    }

    private void handleBeginTridentThrow(@NotNull PlayerPreEatEvent event) {
        var itemStack = event.getItemStack();
        if (itemStack.material().id() != Material.TRIDENT.id())
            return;

        event.setEatingTime(72000);
    }

//    private void handleTridentThrow(@NotNull ItemUpdateStateEvent event) {
//        var itemStack = event.getItemStack();
//        if (itemStack.material().id() != Material.TRIDENT.id())
//            return;
//
//        var riptideLevel = itemStack.get(ItemComponent.ENCHANTMENTS, EnchantmentList.EMPTY)
//                .enchantments().getOrDefault(Enchantment.RIPTIDE, 0);
//        if (riptideLevel > 1) {
//            beginRiptideAttack(event, riptideLevel);
//        } else {
//            throwTridentEntity();
//        }
//    }

    private void throwTridentEntity() {
        //todo actually do the trident throw
    }

//    private void beginRiptideAttack(@NotNull ItemUpdateStateEvent event, int riptideLevel) {
//        event.setRiptideSpinAttack(true);
//
//        var player = event.getPlayer();
//        if (player instanceof PlayerRiptideExtension p) {
//            p.beginRiptideAttack(20);
//
//            var soundEvent = riptideLevel >= 3
//                    ? SoundEvent.ITEM_TRIDENT_RIPTIDE_3
//                    : riptideLevel == 2
//                    ? SoundEvent.ITEM_TRIDENT_RIPTIDE_2
//                    : SoundEvent.ITEM_TRIDENT_RIPTIDE_1;
//            var sound = Sound.sound(soundEvent, Sound.Source.PLAYER, 1.0f, 1.0f);
//            player.getViewersAsAudience().playSound(sound);
//            player.playSound(sound);
//        }
//    }
}
