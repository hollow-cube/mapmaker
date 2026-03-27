package net.hollowcube.mapmaker.cosmetic.impl.particle;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

public class SnowfallParticleImpl extends AbstractParticleImpl {

    public SnowfallParticleImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
    }

    @Override
    public void apply(@NotNull Player player) {
        var task = player.scheduler()
            .submitTask(() -> particleTick(player));
        var lastTask = player.getAndSetTag(PARTICLE_TASK, task);
        if (lastTask != null) lastTask.cancel();
    }

    private @NotNull TaskSchedule particleTick(Player player) {
        player.sendPacketToViewersAndSelf(new ParticlePacket(
            Particle.FALLING_DUST.withBlock(Block.SNOW_BLOCK),
            false, false,
            player.getPosition().withY(y -> y + 2.5),
            new Vec(0.6f, 0.5f, 0.6f), 0f,
            2
        ));

        return TaskSchedule.tick(4);
    }
}
