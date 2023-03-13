package net.hollowcube.chat;

import com.google.auto.service.AutoService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.chat.command.*;
import net.hollowcube.chat.storage.ChatStorage;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.facet.Facet;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerCommandEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

@AutoService(Facet.class)
public class ChatFacet implements Facet {
    private static final System.Logger logger = System.getLogger(ChatFacet.class.getName());
    private static final ServerRuntime runtime = ServerRuntime.getRuntime();

    public static final Tag<UUID> REPLY_TO = Tag.UUID("chat:reply_to");
    public static final Tag<String> CHAT_CHANNEL = Tag.String("chat:channel");

    private final EventNode<Event> eventNode = EventNode.event("hollowcube:chat", EventFilter.ALL, event -> {
                if (event instanceof CancellableEvent cancellableEvent)
                    return !cancellableEvent.isCancelled();
                return true;
            })
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
            try {
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
                }).get();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } else {
            storage = ChatStorage.memory();
        }
    }

    @TestOnly
    ChatFacet(@NotNull ChatStorage storage) {
        this.storage = storage;
    }

    @Override
    public @NotNull ListenableFuture<Void> hook(@NotNull ServerProcess server) {
        server.eventHandler().addChild(eventNode);
        server.command().register(new LogCommand(storage));
        server.command().register(new MessageCommand(this));
        server.command().register(new ReplyCommand(this));
        server.command().register(new StaffChatCommand(this));
        server.command().register(new ChatChannelCommand(this));
        return Futures.immediateVoidFuture();
    }

    public EventNode<Event> eventNode() {
        return eventNode;
    }

    public void sendPrivateMessage(@NotNull Player from, @NotNull Player to, @NotNull String message) {
        var chatMessage = new ChatMessage(
                Instant.now(),
                runtime.hostname(),
                //todo these should be using the players data id, not uuid
                String.join("%s:%s", from.getUuid().toString(), to.getUuid().toString()),
                from.getUuid().toString(),
                message
        );

        Futures.addCallback(
                storage.recordChatMessage(chatMessage),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(Void result) {
                        to.setTag(REPLY_TO, from.getUuid());
                        from.setTag(REPLY_TO, to.getUuid());
                        from.sendMessage("to " + PlayerData.fromPlayer(to).getDisplayName() + ": " + message);
                        to.sendMessage("from " + PlayerData.fromPlayer(from).getDisplayName() + ": " + message);
                    }

                    @Override
                    public void onFailure(@NotNull Throwable t) {
                        logger.log(Level.ERROR, "Error sending private message", t);
                        from.sendMessage("Error sending private message");
                    }
                },
                ForkJoinPool.commonPool()
        );
    }

    private void handleChatEvent(PlayerChatEvent event) {
        var message = new ChatMessage(
                Instant.now(),
                runtime.hostname(),
                ChatMessage.DEFAULT_CONTEXT,
                PlayerData.fromPlayer(event.getPlayer()).getDisplayName(),
                event.getMessage()
        );
        switch (event.getPlayer().getTag(CHAT_CHANNEL)) {
            case ChatMessage.STAFF_CONTEXT:
                sendStaffChatMessage(event.getPlayer(), message.message());
                event.setCancelled(true);
                break;
            case ChatMessage.DEFAULT_CONTEXT:
            default:
                break;
        }
        Futures.addCallback(
                storage.recordChatMessage(message),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(Void result) {
                        //todo send to other servers
                    }

                    @Override
                    public void onFailure(@NotNull Throwable t) {
                        logger.log(Level.ERROR, "Error recording chat message", t);
                    }
                },
                ForkJoinPool.commonPool()
        );
    }

    public void sendStaffChatMessage(@NotNull Player from, @NotNull String message) {
        for (Player target : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            var chatMessage = new ChatMessage(
                    Instant.now(),
                    runtime.hostname(),
                    //todo these should be using the players data id, not uuid
                    String.join("%s:%s", from.getUuid().toString(), target.getUuid().toString()),
                    from.getUuid().toString(),
                    message
            );

            Futures.addCallback(
                    storage.recordChatMessage(chatMessage),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(Void result) {
                            target.sendMessage("[STAFF] " + PlayerData.fromPlayer(from).getDisplayName() + ": " + message);
                        }

                        @Override
                        public void onFailure(@NotNull Throwable t) {
                            logger.log(Level.ERROR, "Error sending staff chat message", t);
                            from.sendMessage("Error sending staff chat message");
                        }
                    },
                    ForkJoinPool.commonPool()
            );
        }
    }

    private void handleCommandEvent(PlayerCommandEvent event) {
        var message = new ChatMessage(
                Instant.now(),
                runtime.hostname(),
                ChatMessage.COMMAND_CONTEXT,
                event.getPlayer().getUuid().toString(),
                event.getCommand()
        );
        Futures.addCallback(
                storage.recordChatMessage(message),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(Void result) {
                        //todo send to other servers
                    }

                    @Override
                    public void onFailure(@NotNull Throwable t) {
                        logger.log(Level.ERROR, "Error recording command", t);
                    }
                },
                ForkJoinPool.commonPool()
        );
    }
}
