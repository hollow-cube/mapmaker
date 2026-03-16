package net.hollowcube.mapmaker.util;

import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;

public class TagCooldown {

    private final long timeout;
    private final Tag<Long> tag;

    public TagCooldown(String name, long timeout) {
        this.timeout = timeout;
        this.tag = Tag.<Long>Transient(name).defaultValue(0L);
    }

    public boolean test(Player player) {
        long now = System.currentTimeMillis();
        long last = player.getTag(tag);
        if (now - last >= timeout) {
            player.setTag(tag, now);
            return true;
        }
        return false;
    }

}
