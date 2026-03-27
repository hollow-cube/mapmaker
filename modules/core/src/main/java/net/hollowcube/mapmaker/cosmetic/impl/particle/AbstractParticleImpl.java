package net.hollowcube.mapmaker.cosmetic.impl.particle;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.impl.CosmeticImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AbstractParticleImpl extends CosmeticImpl {
    public static final Tag<@NotNull Task> PARTICLE_TASK = Tag.Transient("mapmaker:particle_task");

    public AbstractParticleImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
    }

    public static void reset(@NotNull Player player) {
        player.getPlayerMeta().setEffectParticles(List.of());
        player.updateTag(PARTICLE_TASK, task -> {
            if (task != null) task.cancel();
            return null;
        });
    }
}
