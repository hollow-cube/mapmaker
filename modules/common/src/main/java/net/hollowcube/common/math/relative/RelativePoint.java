package net.hollowcube.common.math.relative;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.Contract;

public interface RelativePoint<P extends Point> {

    @Contract(pure = true)
    String x();

    @Contract(pure = true)
    String y();

    @Contract(pure = true)
    String z();

    @Contract(pure = true)
    RelativePoint<P> withX(String x);

    @Contract(pure = true)
    RelativePoint<P> withY(String y);

    @Contract(pure = true)
    RelativePoint<P> withZ(String z);

    @Contract
    P resolve(P origin);
}
