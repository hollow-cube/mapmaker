package net.hollowcube.chat;

import com.google.auto.service.AutoService;
import net.hollowcube.chat.command.LogCommand;
import net.hollowcube.chat.storage.ChatStorage;
import net.hollowcube.server.Facet;
import net.hollowcube.server.ServerWrapper;
import net.hollowcube.util.EventUtil;
import net.hollowcube.util.FutureUtil;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerCommandEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.time.Instant;

@AutoService(Facet.class)
public class ChatFacet implements Facet {

    //todo where does this come from?
    private static final String SERVER_NAME = "test_server";
    private static final String DEFAULT_CHANNEL = "global";
    private static final String COMMAND_CHANNEL = "command";

    private final EventNode<Event> eventNode = EventUtil
            .notCancelledNode("net/hollowcube/chat")
            // Very low priority to run other events which might cancel these beforehand
            .setPriority(-10);

    private final ChatStorage storage;

    public ChatFacet() {
        //todo this should be based on a config param, which should be loaded before loading managers
        this(ChatStorage.noop());
    }

    @TestOnly
    public ChatFacet(@NotNull ChatStorage storage) {
        this.storage = storage;

        eventNode.addListener(PlayerChatEvent.class, this::handleChatEvent);
        eventNode.addListener(PlayerCommandEvent.class, this::handleCommandEvent);
    }

    @Override
    public void hook(@NotNull ServerWrapper server) {
        server.addEventNode(eventNode);
        server.registerCommand(new LogCommand(storage));
    }

    public EventNode<Event> eventNode() {
        return eventNode;
    }

    private void handleChatEvent(PlayerChatEvent event) {
        // Record message, ignore response.
        storage.recordChatMessage(new ChatMessage(
                Instant.now(),
                SERVER_NAME,
                DEFAULT_CHANNEL,
                event.getPlayer().getUuid(),
                event.getMessage()
        )).exceptionally(FutureUtil::handleException);
    }

    private void handleCommandEvent(PlayerCommandEvent event) {
        // Record command, ignore response.
        storage.recordChatMessage(new ChatMessage(
                Instant.now(),
                SERVER_NAME,
                COMMAND_CHANNEL,
                event.getPlayer().getUuid(),
                event.getCommand()
        )).exceptionally(FutureUtil::handleException);
    }
}
