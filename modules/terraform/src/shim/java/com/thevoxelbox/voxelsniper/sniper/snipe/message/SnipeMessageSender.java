package com.thevoxelbox.voxelsniper.sniper.snipe.message;

import com.sk89q.worldedit.util.formatting.text.Component;

public record SnipeMessageSender(Component component) {

    public SnipeMessageSender() {
        this(null);
    }

    public SnipeMessageSender message(Component component) {
        return new SnipeMessageSender(component);
    }

    public void send() {
        System.out.println("tried to send message: " + component);
    }


}
