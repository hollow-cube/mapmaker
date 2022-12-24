package net.hollowcube.chat;

import com.google.auto.service.AutoService;
import net.hollowcube.chat.command.LogCommand;
import net.hollowcube.chat.storage.ChatStorage;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.facet.Facet;
import net.hollowcube.util.EventUtil;
import net.minestom.server.ServerProcess;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerCommandEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.System.Logger.Level;
import java.time.Instant;

@AutoService(Facet.class)
public class ChatFacet implements Facet {
    private static final System.Logger logger = System.getLogger(ChatFacet.class.getName());
    private static final ServerRuntime runtime = ServerRuntime.getRuntime();

    private final EventNode<Event> eventNode = EventUtil
            .notCancelledNode("hollowcube:chat")
            // Very low priority to run other events which might cancel these beforehand
            .setPriority(-10)
            .addListener(PlayerChatEvent.class, this::handleChatEvent)
            .addListener(PlayerCommandEvent.class, this::handleCommandEvent);

    private final ChatStorage storage;

    public ChatFacet() {
        var mongoUri = System.getenv("MM_MONGO_URI");
        if (mongoUri != null) {
            storage = ChatStorage.mongo(mongoUri);
        } else {
            storage = ChatStorage.noop();
        }
    }

    @Override
    public void hook(@NotNull ServerProcess server) {
        server.eventHandler().addChild(eventNode);
        server.command().register(new LogCommand(storage));
    }

    public EventNode<Event> eventNode() {
        return eventNode;
    }

    private void handleChatEvent(PlayerChatEvent event) {
        storage.recordChatMessage(new ChatMessage(
                Instant.now(),
                runtime.workerId(),
                ChatMessage.DEFAULT_CONTEXT,
                event.getPlayer().getUuid(),
                event.getMessage()
        )).thenErr(err -> logger.log(Level.ERROR, "failed to record chat message: {}", err.message()));
    }

    private void handleCommandEvent(PlayerCommandEvent event) {
        storage.recordChatMessage(new ChatMessage(
                Instant.now(),
                runtime.workerId(),
                ChatMessage.COMMAND_CONTEXT,
                event.getPlayer().getUuid(),
                event.getCommand()
        )).thenErr(err -> logger.log(Level.ERROR, "failed to record command: {}", err.message()));
    }
}
