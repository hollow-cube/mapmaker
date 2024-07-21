package net.hollowcube.mapmaker.hub.anim;

import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public class ChannelImpl {
    static class Position implements Channel<Pos> {
        record Value(@NotNull Pos vec) implements Channel.Value {
            @Override
            public @NotNull Channel<?> channel() {
                return Channel.POSITION;
            }

            @Override
            public void apply(@NotNull InterpolationHelper entity) {
                entity.setPosition(vec);
//                entity.refreshPosition(vec, false, true);
            }

            @Override
            public Vec lerp(Channel.@NotNull Value other, float t) {
                return Vec.fromPoint(CoordinateUtil.lerp(vec, ((Value) other).vec, t));
            }
        }

        private final Channel.Value defaultValue = new Value(Pos.ZERO);

        @Override
        public Channel.@NotNull Value defaultValue() {
            return defaultValue;
        }

        @Override
        public @NotNull Channel.Value set(@NotNull Pos value) {
            return new Value(value);
        }
    }

    static class Translation implements Channel<Point> {
        record Value(@NotNull Point vec) implements Channel.Value {
            @Override
            public @NotNull Channel<?> channel() {
                return Channel.TRANSLATION;
            }

            @Override
            public void apply(@NotNull InterpolationHelper entity) {
                entity.setTranslation(vec);
            }
        }

        private final Channel.Value defaultValue = new Value(new Vec(0, 0, 0));

        @Override
        public Channel.@NotNull Value defaultValue() {
            return defaultValue;
        }

        @Override
        public @NotNull Channel.Value set(@NotNull Point value) {
            return new Value(value);
        }
    }

}
