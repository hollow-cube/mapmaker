package net.hollowcube.chat;

import net.hollowcube.chat.storage.MockChatStorage;
import net.hollowcube.test.TestUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestChatFacet {
    private final MockChatStorage storage = new MockChatStorage();
    private final ChatFacet manager = new ChatFacet(storage);
    private final Player player = TestUtil.headlessPlayer();

    @Test
    public void testAddChatMessage() {

        PlayerChatEvent event = new PlayerChatEvent(
                player,
                Collections.emptyList(),
                () -> Component.text(""),
                "test message 1"
        );

        // Call without cancellation even though it is cancellable.
        manager.eventNode().call(event);

        assertFalse(event.isCancelled());
        ChatMessage message = storage.assertOneMessage();
        assertEquals(player.getUuid().toString(), message.sender());
        assertEquals("global", message.context());
        assertEquals("test message 1", message.message());
    }

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
