package net.hollowcube.terraform.cui.meow.lines;

import net.kyori.adventure.util.RGBLike;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;

public class DefaultLine extends AbstractLine {
    public DefaultLine(
            Player player,
            Point from,
            Point to,
            RGBLike color
    ) {
        super(player, from, to, color);
    }
}
