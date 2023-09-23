package net.hollowcube.test.subject;

import net.minestom.server.ServerProcess;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TestPlayer extends Player {
    private final ServerProcess process;

    public TestPlayer(@NotNull ServerProcess process, @NotNull PlayerConnection playerConnection) {
        super(UUID.randomUUID(), "TestPlayer", playerConnection);

        this.process = process;
    }

    public void executeCommand(@NotNull String command) {
        process.command().execute(this, command);
    }
}
