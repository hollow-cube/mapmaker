package net.hollowcube.mapmaker.cosmetic.impl.particle;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.impl.CosmeticImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AbstractParticleImpl extends CosmeticImpl {
    public static final Tag<@Nullable Task> PARTICLE_TASK = Tag.Transient("mapmaker:particle_task");

    public AbstractParticleImpl(Cosmetic cosmetic) {
        super(cosmetic);
    }

    public static void reset(Player player) {
        player.getPlayerMeta().setEffectParticles(List.of());
        player.updateTag(PARTICLE_TASK, task -> {
            if (task != null) task.cancel();
            //noinspection ReturnOfNull - this meets the contract of updateTag
            return null;
        });
    }
}
