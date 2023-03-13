package net.hollowcube.chat;

import net.hollowcube.chat.storage.MockChatStorage;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.UUID;

public class TestChatFacet {
    private final MockChatStorage storage = new MockChatStorage();
    private final ChatFacet manager = new ChatFacet(storage);
    public static final Player player = new Player(UUID.randomUUID(), "test", new PlayerConnection() {
        //todo replace me with common headless player
        @Override
        public void sendPacket(@NotNull SendablePacket packet) {

        }

        @Override
        public @NotNull SocketAddress getRemoteAddress() {
            return new InetSocketAddress("localhost", 25565);
        }
    });

//    static {
//        MinecraftServer.init();
//    }

//    @Test
//    public void testAddChatMessage() {
//        PlayerChatEvent event = new PlayerChatEvent(
//                player,
//                Collections.emptyList(),
//                () -> Component.text(""),
//                "test message 1"
//        );
//
//        // Call without cancellation even though it is cancellable.
//        manager.eventNode().call(event);
//
//        assertFalse(event.isCancelled());
//        ChatMessage message = storage.assertOneMessage();
//        assertEquals(player.getUuid().toString(), message.sender());
//        assertEquals("global", message.context());
//        assertEquals("test message 1", message.message());
//    }

    @Test
    public void testIgnoreCancelledChatEvents() {

        PlayerChatEvent event = new PlayerChatEvent(
                player,
                Collections.emptyList(),
                () -> Component.text(""),
                "test message 1"
        );

        // Call without cancellation even though it is cancellable.
        event.setCancelled(true);
        manager.eventNode().call(event);

        storage.assertEmpty();
    }

}
