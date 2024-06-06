package com.sk89q.worldedit.extension.input;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;

public class ParserContext {
    private World world;
    private LocalSession session;
    private Extent extent;

    public void setWorld(World world) {
        this.world = world;
    }

    public void setSession(LocalSession session) {
        this.session = session;
    }

    public void setExtent(Extent extent) {
        this.extent = extent;
    }
}
