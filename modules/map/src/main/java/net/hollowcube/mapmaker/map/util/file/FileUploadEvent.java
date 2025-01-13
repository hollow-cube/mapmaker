package net.hollowcube.mapmaker.map.util.file;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileUploadEvent implements PlayerInstanceEvent {
    private final Player player;
    private final int action;
    private final String type;
    private final String name;
    private final byte[] data;

    private boolean handled;
    private String error;

    public FileUploadEvent(@NotNull Player player, int action, @NotNull String type, @NotNull String name, byte @NotNull [] data) {
        this.player = player;
        this.action = action;
        this.type = type;
        this.name = name;
        this.data = data;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public int action() {
        return action;
    }

    public @NotNull String type() {
        return type;
    }

    public @NotNull String name() {
        return name;
    }

    public byte[] data() {
        return data;
    }

    public boolean isHandled() {
        return handled;
    }

    public String error() {
        return error;
    }

    public void setHandled(@Nullable String error) {
        this.handled = true;
        this.error = error;
    }
}
