package net.hollowcube.mapmaker.cosmetic.impl.victory;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ParticleVictoryEffects {

    public static class ChristmasExplosion extends ParticleExplosionVictoryEffectImpl {

        public ChristmasExplosion(@NotNull Cosmetic cosmetic) {
            super(
                    cosmetic,
                    Map.of(
                            Particle.SNOWFLAKE, 50,
                            Particle.ITEM.withItem(ItemStack.of(Material.RED_CONCRETE)), 100,
                            Particle.ITEM.withItem(ItemStack.of(Material.GREEN_CONCRETE)), 100,
                            Particle.END_ROD, 50,
                            Particle.FLASH.withColor(50, NamedTextColor.RED), 1,
                            Particle.FLASH, 1
                    ),
                    0.2f
            );
        }

        @Override
        protected void explode(@NotNull Entity entity, @NotNull Point position) {
            entity.sendPacketToViewersAndSelf(new SoundEffectPacket(
                    SoundEvent.ENTITY_GENERIC_EXPLODE,
                    Sound.Source.PLAYER,
                    position,
                    0.05f, 2f,
                    entity.getEntityId()
            ));
            entity.sendPacketToViewersAndSelf(new SoundEffectPacket(
                    SoundEvent.BLOCK_AMETHYST_BLOCK_CHIME,
                    Sound.Source.PLAYER,
                    position,
                    10f, 2f,
                    entity.getEntityId()
            ));
            super.explode(entity, position);
        }
    }
}
