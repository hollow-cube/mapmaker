package net.hollowcube.mapmaker.cosmetic.impl.victory;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;

import java.util.Map;

public class ParticleExplosionVictoryEffectImpl extends AbstractVictoryEffectImpl {

    private final Object2IntMap<Particle> particles;
    private final float speed;

    public ParticleExplosionVictoryEffectImpl(Cosmetic cosmetic, Map<Particle, Integer> particles, float speed) {
        this(cosmetic, new Object2IntArrayMap<>(particles), speed);
    }

    public ParticleExplosionVictoryEffectImpl(Cosmetic cosmetic, Object2IntMap<Particle> particles, float speed) {
        super(cosmetic);
        this.particles = particles;
        this.speed = speed;
    }

    @Override
    public void trigger(Player player, Point position) {
        var realPosition = position.add(0, player.getBoundingBox().height() / 2f, 0);
        player.scheduleNextTick(entity -> explode(entity, realPosition));
    }

    protected void explode(Entity entity, Point position) {
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
