package net.hollowcube.mapmaker.cosmetic.impl.particle;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.impl.CosmeticImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public class DefaultParticleImpl extends AbstractParticleImpl {

    private final List<Particle> particles;

    public DefaultParticleImpl(@NotNull Cosmetic cosmetic, @NotNull List<Particle> particles) {
        super(cosmetic);
        this.particles = particles;
    }

    @Override
    public void apply(@NotNull Player player) {
        player.getPlayerMeta().setEffectParticles(this.particles);
    }

    public static Function<Cosmetic, CosmeticImpl> of(Particle particle) {
        return cosmetic -> new DefaultParticleImpl(cosmetic, List.of(particle));
    }
}
