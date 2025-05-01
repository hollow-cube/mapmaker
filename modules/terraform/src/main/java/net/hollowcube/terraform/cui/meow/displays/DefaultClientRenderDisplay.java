package net.hollowcube.terraform.cui.meow.displays;

import net.hollowcube.terraform.cui.meow.lines.AbstractLine;
import net.hollowcube.terraform.cui.meow.lines.AxisAlignedLine;
import net.hollowcube.terraform.cui.meow.lines.DefaultLine;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;

public interface DefaultClientRenderDisplay {
    default AbstractLine drawAxis(Player player, Point pos1, Point pos2, RGBLike color) {
        return new AxisAlignedLine(player, pos1, pos2, color);
    }

    default AbstractLine drawLine(Player player, Point pos1, Point pos2, RGBLike color) {
        return new DefaultLine(player, pos1, pos2, color);
    }

    void removeDisplay();

    void hide();
    void show();
}
