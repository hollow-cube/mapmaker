package net.hollowcube.chat;

import com.google.auto.service.AutoService;
import net.hollowcube.chat.command.LogCommand;
import net.hollowcube.chat.command.MessageCommand;
import net.hollowcube.chat.command.ReplyCommand;
import net.hollowcube.chat.storage.ChatStorage;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.facet.Facet;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.util.EventUtil;
import net.minestom.server.ServerProcess;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerCommandEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.UUID;

@AutoService(Facet.class)
public class ChatFacet implements Facet {
    private static final System.Logger logger = System.getLogger(ChatFacet.class.getName());
    private static final ServerRuntime runtime = ServerRuntime.getRuntime();

    public static final Tag<UUID> REPLY_TO = Tag.UUID("chat:reply_to");

    private final EventNode<Event> eventNode = EventUtil
            .notCancelledNode("hollowcube:chat")
            // Very low priority to run other events which might cancel these beforehand
            .setPriority(-10)
            .addListener(PlayerChatEvent.class, this::handleChatEvent)
            .addListener(PlayerCommandEvent.class, this::handleCommandEvent);

    private final ChatStorage storage;

    public ChatFacet() {
        //todo need to have futureresult init for this
        //todo need to give access to MongoConfig somehow here
        var mongoUri = System.getenv("MM_MONGO_URI");
        if (mongoUri != null) {
            storage = ChatStorage.mongo(new MongoConfig() {
                @Override
                public @NotNull String uri() {
                    return mongoUri;
                }

                @Override
                public @NotNull String database() {
                    return "mapmaker";
                }

                @Override
                public boolean useTransactions() {
                    return false;
                }
            }).toCompletableFuture().join().result();
        } else {
            storage = ChatStorage.noop();
        }
    }

    @Override
    public @NotNull FutureResult<Void> hook(@NotNull ServerProcess server) {
        server.eventHandler().addChild(eventNode);
        server.command().register(new LogCommand(storage));
        server.command().register(new MessageCommand(this));
        server.command().register(new ReplyCommand(this));
        return FutureResult.ofNull();
    }

    public EventNode<Event> eventNode() {
        return eventNode;
    }

    public void sendPrivateMessage(@NotNull Player from, @NotNull Player to, @NotNull String message) {
        var chatMessage = new ChatMessage(
                Instant.now(),
                runtime.workerId(),
                //todo these should be using the players data id, not uuid
                String.join("%s:%s", from.getUuid().toString(), to.getUuid().toString()),
                from.getUuid(),
                message
        );

        storage.recordChatMessage(chatMessage)
                .then(unused -> {
                    to.setTag(REPLY_TO, from.getUuid());
                    from.setTag(REPLY_TO, to.getUuid());
                    from.sendMessage("to " + to.getUsername() + ": " + message);
                    to.sendMessage("from " + from.getUsername() + ": " + message);
                })
                .thenErr(err -> {
                    logger.log(Level.ERROR, "Error sending private message", err);
                    from.sendMessage("Error sending private message");
                });
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
