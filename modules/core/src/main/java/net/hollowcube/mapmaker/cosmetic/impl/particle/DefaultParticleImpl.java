package net.hollowcube.mapmaker.cosmetic.impl.particle;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.impl.CosmeticImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.particle.Particle;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class DefaultParticleImpl extends AbstractParticleImpl {

    private final List<Particle> particles;

    public DefaultParticleImpl(Cosmetic cosmetic, List<Particle> particles) {
        super(cosmetic);
        this.particles = particles;
    }

    @Override
    public void apply(Player player) {
        player.getPlayerMeta().setEffectParticles(this.particles);
    }

    // Note vanilla has a 1/4 chance of showing the particle, so we add on to that.
    // Vanilla is also a 1/15 if the player is invisible
    public static Function<Cosmetic, CosmeticImpl> of(Particle particle, float chance) {
        int emptyParticles = Math.round((1 - chance) / chance);
        var particles = new Particle[emptyParticles + 1];
        particles[0] = particle;
        Arrays.fill(particles, 1, particles.length, Particle.BLOCK.withBlock(Block.AIR));
        return cosmetic -> new DefaultParticleImpl(cosmetic, List.of(particles));
    }
}
