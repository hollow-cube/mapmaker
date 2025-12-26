package net.hollowcube.mapmaker.hub.feature.event.newyears;

import java.util.List;

public class FireworkShapes {

    public static final List<CustomFirework.Line> TWO = List.of(
        new CustomFirework.Line(-1.25, 2, 1.25, 2),
        new CustomFirework.Line(-1.25, 1, -1.25, 2),
        new CustomFirework.Line(-1.25, 1, 0, 1),
        new CustomFirework.Line(0, 0.25, 0, 1),
        new CustomFirework.Line(-1.25, 0.25, 0, 0.25),
        new CustomFirework.Line(-1.25, -2, -1.25, 0.25),
        new CustomFirework.Line(1.25, -0.5, 1.25, 2),
        new CustomFirework.Line(0, -0.5, 1.25, -0.5),
        new CustomFirework.Line(0, -1.25, 0, -0.5),
        new CustomFirework.Line(0, -1.25, 1.25, -1.25),
        new CustomFirework.Line(1.25, -2, 1.25, -1.25),
        new CustomFirework.Line(-1.25, -2, 1.25, -2)
    );

    public static final List<CustomFirework.Line> ZERO = List.of(
        new CustomFirework.Line(-1.25, 2, 1.25, 2),
        new CustomFirework.Line(-1.25, -2, -1.25, 2),
        new CustomFirework.Line(1.25, -2, 1.25, 2),
        new CustomFirework.Line(-1.25, -2, 1.25, -2),
        new CustomFirework.Line(-0.5, 1.25, 0.5, 1.25),
        new CustomFirework.Line(-0.5, 1.25, -0.5, -1.25),
        new CustomFirework.Line(-0.5, -1.25, 0.5, -1.25),
        new CustomFirework.Line(0.5, 1.25, 0.5, -1.25)
    );

    public static final List<CustomFirework.Line> SIX = List.of(
        new CustomFirework.Line(-1.25, 2, 1.25, 2),
        new CustomFirework.Line(-1.25, 2, -1.25, -2),
        new CustomFirework.Line(-0.5, 1.25, 1.25, 1.25),
        new CustomFirework.Line(1.25, 1.25, 1.25, 2),
        new CustomFirework.Line(-0.5, 1.25, -0.5, 0.25),
        new CustomFirework.Line(-0.5, 0.25, 1.25, 0.25),
        new CustomFirework.Line(1.25, -2, 1.25, 0.25),
        new CustomFirework.Line(-1.25, -2, 1.25, -2),
        new CustomFirework.Line(-0.5, -1.25, 0.5, -1.25),
        new CustomFirework.Line(-0.5, -0.5, 0.5, -0.5),
        new CustomFirework.Line(-0.5, -0.5, -0.5, -1.25),
        new CustomFirework.Line(0.5, -0.5, 0.5, -1.25)
    );
}
