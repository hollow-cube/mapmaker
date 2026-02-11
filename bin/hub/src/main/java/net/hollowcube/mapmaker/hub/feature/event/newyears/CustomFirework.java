package net.hollowcube.mapmaker.hub.feature.event.newyears;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;

import java.util.List;

public class CustomFirework extends Firework {

    private static final double SPREAD_FACTOR = 0.25;

    private final List<Line> lines;
    private double rotation = 0.0;

    public CustomFirework(int ticks, List<Line> lines) {
        super(ticks);

        this.lines = lines;
    }

    public CustomFirework withRotation(double rotation) {
        this.rotation = rotation;
        return this;
    }

    @Override
    protected void explode() {
        var position = this.getPosition();
        for (var edge : this.lines) {
            edge.spawn(this, position);
        }
        this.remove();
    }

    public record Line(double minX, double minY, double maxX, double maxY) {

        private double clampedLerp(double start, double end, double progress) {
            if (progress < 0) return start;
            if (progress > 1) return end;
            return start + (end - start) * progress;
        }

        void spawn(CustomFirework firework, Point offset) {
            var distance = Math.sqrt(Math.pow(maxX - minX, 2) + Math.pow(maxY - minY, 2));
            var count = distance / SPREAD_FACTOR;

            var cos = Math.cos(firework.rotation);
            var sin = Math.sin(firework.rotation);

            for (double i = 0; i <= count; i++) {
                var progress = i / count;
                var localX = clampedLerp(minX, maxX, progress);
                var localY = clampedLerp(minY, maxY, progress);

                var x = offset.x() + (localX * cos);
                var y = offset.y() + localY;
                var z = offset.z() + (localX * sin);

                firework.sendPacketToViewers(new ParticlePacket(
                    Particle.END_ROD,
                    true, true,
                    x, y, z,
                    0, 0, 0,
                    0f, 1
                ));
            }
        }
    }
}
