package modules.anticheat.src.main.java.net.hollowcube.anticheat.utils;

import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

public final class TagTimer {

    private final long timeout;
    private final Tag<Long> tag;

    public TagTimer(@NotNull String name, long timeout) {
        this.timeout = timeout;
        this.tag = Tag.<Long>Transient(name).defaultValue(0L);
    }

    public void update(@NotNull Player player) {
        player.setTag(tag, System.currentTimeMillis());
    }

    public boolean test(@NotNull Player player) {
        long now = System.currentTimeMillis();
        long last = player.getTag(tag);
        if (now - last >= timeout) {
            player.setTag(tag, now);
            return true;
        }
        return false;
    }

}