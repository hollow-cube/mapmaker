package com.sk89q.worldedit.session.request;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.world.World;

public class Request {

    public static Request request() {
        return new Request();
    }

    public World getWorld() {
        return null;
    }

    public LocalSession getSession() {
        return null;
    }

    public EditSession getEditSession() {
        return null;
    }
}
