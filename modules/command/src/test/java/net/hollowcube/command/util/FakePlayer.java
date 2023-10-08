package net.hollowcube.command.util;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

public class FakePlayer extends Player {
    public static final CommandSender CONSOLE = new ConsoleSender();

    public FakePlayer() {
        super(UUID.randomUUID(), "random", new PlayerConnection() {
            @Override
            public void sendPacket(@NotNull SendablePacket packet) {

            }

            @Override
            public @NotNull SocketAddress getRemoteAddress() {
                return new InetSocketAddress(0);
            }
        });
    }
}
