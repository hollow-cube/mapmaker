package net.hollowcube.test.subject;

import net.hollowcube.test.TestEnvImpl;
import net.minestom.server.ServerProcess;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

public class TestConnection {
    private final TestEnvImpl env;
    private final ServerProcess process;
    private final PlayerConnectionImpl playerConnection = new PlayerConnectionImpl();

    public TestConnection(TestEnvImpl env) {
        this.env = env;
        this.process = env.process();
    }

    public @NotNull CompletableFuture<TestPlayer> connect(@NotNull Instance instance, @NotNull Pos pos) {
        TestPlayer player = new TestPlayer(process, playerConnection);
        player.eventNode().addListener(PlayerLoginEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(pos);
        });

        return process.connection().startPlayState(player, true)
                .thenApply(unused -> {
                    process.connection().updateWaitingPlayers();
                    return player;
                })
                .thenApply(unused -> {
                    player.setInstance(instance).join();
                    return player;
                });
    }

    final class PlayerConnectionImpl extends PlayerConnection {
        @Override
        public void sendPacket(@NotNull SendablePacket packet) {
        }

        @Override
        public @NotNull SocketAddress getRemoteAddress() {
            return new InetSocketAddress("localhost", 25565);
        }

        @Override
        public void disconnect() {

        }
    }
}
