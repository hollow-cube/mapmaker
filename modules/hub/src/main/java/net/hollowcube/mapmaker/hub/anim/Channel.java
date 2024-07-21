package net.hollowcube.mapmaker.hub.anim;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Channel<T> {

    Channel<Pos> POSITION = new ChannelImpl.Position();
    Channel<Point> TRANSLATION = new ChannelImpl.Translation();
//    Channel<Quaternion> LEFT_ROTATION = new ChannelImpl.LeftRotation();

    static @NotNull List<Channel<?>> values() {
        class Holder {
            static final List<Channel<?>> VALUES = List.of(POSITION, TRANSLATION);
        }
        return Holder.VALUES;
    }

    @NotNull Value defaultValue();

    @NotNull Value set(@NotNull T value);

    interface Value {
        @NotNull Channel<?> channel();

        void apply(@NotNull InterpolationHelper entity);

        default Vec lerp(@NotNull Value other, float t) {
            return Vec.ZERO;
        }
    }

}
