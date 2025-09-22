package net.hollowcube.mapmaker.cosmetic.impl.particle;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.impl.CosmeticImpl;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AbstractParticleImpl extends CosmeticImpl {

    public AbstractParticleImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
    }

    public static void reset(@NotNull Player player) {
        player.getPlayerMeta().setEffectParticles(List.of());
    }
}
