package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.mapmaker.backpack.Rarity;
import net.hollowcube.mapmaker.cosmetic.impl.particle.DefaultParticleImpl;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.NotNull;

public final class Particles {

    public static Cosmetic CLOUD = builder("cloud").rarity(Rarity.COMMON).build();
    public static Cosmetic BUBBLE = builder("bubble").rarity(Rarity.COMMON).build();
    public static Cosmetic NOTE = builder("note").rarity(Rarity.COMMON).build();
    public static Cosmetic CHERRY_LEAVES = builder("cherry_leaves")
            .rarity(Rarity.COMMON)
            .impl(DefaultParticleImpl.of(Particle.CHERRY_LEAVES))
            .build();

    public static @NotNull Cosmetic.Builder builder(@NotNull String id) {
        return new Cosmetic.Builder(CosmeticType.PARTICLE, id);
    }
}
