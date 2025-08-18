package net.hollowcube.mapmaker.map.item.vanilla;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

public class TridentItem extends VanillaItemHandler {
    public static final TridentItem INSTANCE = new TridentItem();

    public static @NotNull ItemStack get(int riptide) {
        var stack = TridentItem.INSTANCE.getItemStack(1);
        stack = stack.with(DataComponents.ENCHANTMENTS,
                EnchantmentList.EMPTY.with(Enchantment.RIPTIDE, riptide));
        return stack;
    }

    private TridentItem() {
        super(Material.TRIDENT, false, CONSUME_ITEM);
    }

    @Override
    protected int beginConsume(@NotNull Click click) {
        return isWet(click.player()) ? 72000 : -1;
    }

    @Override
    protected ConsumeItemResult cancelConsume(@NotNull Click click, long duration) {
        if (duration < 10) return ConsumeItemResult.CANCEL;

        var riptideLevel = click.itemStack().get(DataComponents.ENCHANTMENTS, EnchantmentList.EMPTY)
                .enchantments().getOrDefault(Enchantment.RIPTIDE, 0);
        if (riptideLevel >= 1) {
            return beginRiptideAttack(click.player(), riptideLevel);
        } else {
            return throwTridentEntity();
        }
    }

    private ConsumeItemResult throwTridentEntity() {
        //todo actually do the trident throw
        return ConsumeItemResult.CANCEL;
    }

    private ConsumeItemResult beginRiptideAttack(@NotNull Player player, int riptideLevel) {
        if (!isWet(player)) return ConsumeItemResult.CANCEL;

        if (player instanceof MapPlayer mp) {
            mp.beginRiptideAttack(20);

            var soundEvent = switch (riptideLevel) {
                case 1 -> SoundEvent.ITEM_TRIDENT_RIPTIDE_1;
                case 2 -> SoundEvent.ITEM_TRIDENT_RIPTIDE_2;
                default -> SoundEvent.ITEM_TRIDENT_RIPTIDE_3;
            };
            var sound = Sound.sound(soundEvent, Sound.Source.PLAYER, 1.0f, 1.0f);
            player.getViewersAsAudience().playSound(sound, player);
            player.playSound(sound);
        }

        return ConsumeItemResult.RIPTIDE_SPIN;
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
        return player instanceof MapPlayer mp && mp.isInWater();
    }

}
