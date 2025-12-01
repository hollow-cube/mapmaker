package net.hollowcube.mapmaker.cosmetic.impl.victory;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.impl.CosmeticImpl;
import net.hollowcube.mapmaker.cosmetic.impl.particle.DefaultParticleImpl;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.BuiltinSoundEvent;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ParticleExplosionVictoryEffectImpl extends AbstractVictoryEffectImpl {

    private final Object2IntMap<Particle> particles;
    private final float speed;

    public ParticleExplosionVictoryEffectImpl(
            @NotNull Cosmetic cosmetic,
            @NotNull Map<Particle, Integer> particles,
            float speed
    ) {
        this(cosmetic, new Object2IntArrayMap<>(particles), speed);
    }

    public ParticleExplosionVictoryEffectImpl(
            @NotNull Cosmetic cosmetic,
            @NotNull Object2IntMap<Particle> particles,
            float speed
    ) {
        super(cosmetic);
        this.particles = particles;
        this.speed = speed;
    }

    @Override
    public void trigger(@NotNull Player player, @NotNull Point position) {
        var realPosition = position.add(0, player.getBoundingBox().height() / 2f, 0);
        player.scheduleNextTick(entity -> explode(entity, realPosition));
    }

    protected void explode(@NotNull Entity entity, @NotNull Point position) {
        for (var entry : particles.object2IntEntrySet()) {
            entity.sendPacketToViewersAndSelf(new ParticlePacket(
                    entry.getKey(),
                    false,
                    false,
                    position,
                    Vec.ZERO,
                    speed,
                    entry.getIntValue()
            ));
        }
    }
}
