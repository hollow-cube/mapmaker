package net.hollowcube.mapmaker.map.feature.play.vanilla;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.mapmaker.map.util.PlayerLiquidExtension;
import net.hollowcube.mapmaker.map.util.PlayerRiptideExtension;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.PlayerBeginItemUseEvent;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import net.minestom.server.event.player.PlayerTickEndEvent;
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
            .addListener(PlayerBeginItemUseEvent.class, this::handleBeginTridentThrow)
            .addListener(PlayerCancelItemUseEvent.class, this::handleTridentThrow);

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

    private void handleBeginTridentThrow(@NotNull PlayerBeginItemUseEvent event) {
        var itemStack = event.getItemStack();
        if (itemStack.material().id() != Material.TRIDENT.id())
            return;

        if (!isWet(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }

        event.setItemUseDuration(72000);
    }

    private void handleTridentThrow(@NotNull PlayerCancelItemUseEvent event) {
        var itemStack = event.getItemStack();
        if (itemStack.material().id() != Material.TRIDENT.id() || event.getUseDuration() < 10)
            return;

        var riptideLevel = itemStack.get(ItemComponent.ENCHANTMENTS, EnchantmentList.EMPTY)
                .enchantments().getOrDefault(Enchantment.RIPTIDE, 0);
        if (riptideLevel > 1) {
            beginRiptideAttack(event, riptideLevel);
        } else {
            throwTridentEntity();
        }
    }

    private void throwTridentEntity() {
        //todo actually do the trident throw
    }

    private void beginRiptideAttack(@NotNull PlayerCancelItemUseEvent event, int riptideLevel) {
        event.setRiptideSpinAttack(true);

        var player = event.getPlayer();
        if (player instanceof PlayerRiptideExtension p) {
            p.beginRiptideAttack(20);

            var soundEvent = switch (riptideLevel) {
                case 1 -> SoundEvent.ITEM_TRIDENT_RIPTIDE_1;
                case 2 -> SoundEvent.ITEM_TRIDENT_RIPTIDE_2;
                default -> SoundEvent.ITEM_TRIDENT_RIPTIDE_3;
            };
            var sound = Sound.sound(soundEvent, Sound.Source.PLAYER, 1.0f, 1.0f);
            player.getViewersAsAudience().playSound(sound, player);
            player.playSound(sound);
        }
    }

    private static boolean isWet(@NotNull Player player) {
        return isRaining(player) || isInWater(player);
    }

    private static boolean isRaining(@NotNull Player player) {
        var position = player.getPosition();
        var isRaining = player.getInstance().getWeather().isRaining();
        if (!isRaining) return false;

        if (!(player.getChunk() instanceof ChunkExt chunk))
            return false;

        return chunk.getHeight(Heightmaps.MOTION_BLOCKING, position) <= position.y();
    }

    private static boolean isInWater(@NotNull Player player) {
        return player instanceof PlayerLiquidExtension ple && ple.isInWater();
    }
}
