package net.hollowcube.aj.bone;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBone {
    private AbstractBone parent = null;
    private final List<AbstractBone> children = new ArrayList<>();

    private Vec position = Vec.ZERO;
    private Vec rotation = Vec.ZERO;
    private Vec scale = Vec.ONE;

    public void addChild(@NotNull AbstractBone child) {
        children.add(child);
        child.parent = this;
    }

    public void updateNewViewer(@NotNull Player player) {
        for (var child : this.children) {
            child.updateNewViewer(player);
        }
    }

}
