package net.hollowcube.chat;

import com.google.auto.service.AutoService;
import net.hollowcube.chat.command.*;
import net.hollowcube.chat.storage.ChatStorage;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.config.ConfigProvider;
import net.hollowcube.common.config.MongoConfigNew;
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
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
@AutoService(Facet.class)
public class ChatFacet implements Facet {
    //todo the threading in this thing is all messed up. i think i need to make the chat event run async, which in theory should be fine.

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

    private ChatStorage storage;

    public ChatFacet() {
    }

    @TestOnly
    ChatFacet(@NotNull ChatStorage storage) {
        this.storage = storage;
    }

    @Override
    public void hook(@NotNull ServerProcess server, @NotNull ConfigProvider config) {
        if (System.getenv("MM_CHAT_STORAGE_DEV") != null) {
            this.storage = ChatStorage.memory();
        } else {
            var mongoConf = config.get(MongoConfigNew.class);
            this.storage = ChatStorage.mongo(mongoConf);
        }

        server.eventHandler().addChild(eventNode);
        server.command().register(new LogCommand(storage));
        server.command().register(new MessageCommand(this));
        server.command().register(new ReplyCommand(this));
        server.command().register(new StaffChatCommand(this));
        server.command().register(new ChatChannelCommand(this));
    }

    public EventNode<Event> eventNode() {
        return eventNode;
    }

    public @Blocking void sendPrivateMessage(@NotNull Player from, @NotNull Player to, @NotNull String message) {
        var chatMessage = new ChatMessage(
                Instant.now(),
                runtime.hostname(),
                //todo these should be using the players data id, not uuid
                String.join("%s:%s", from.getUuid().toString(), to.getUuid().toString()),
                from.getUuid().toString(),
                message
        );

        try {
            storage.recordChatMessage(chatMessage);
            to.setTag(REPLY_TO, from.getUuid());
            from.setTag(REPLY_TO, to.getUuid());
            from.sendMessage("to " + PlayerData.fromPlayer(to).getDisplayName() + ": " + message);
            to.sendMessage("from " + PlayerData.fromPlayer(from).getDisplayName() + ": " + message);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Error sending private message", e);
            from.sendMessage("Error sending private message");
        }
    }

    private @NonBlocking void handleChatEvent(PlayerChatEvent event) {
        var senderData = PlayerData.fromPlayer(event.getPlayer());
        var message = new ChatMessage(
                Instant.now(),
                runtime.hostname(),
                ChatMessage.DEFAULT_CONTEXT,
                senderData.getId(),
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
        try {
            storage.recordChatMessage(message);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Error recording chat message", e);
        }
    }

    public @Blocking void sendStaffChatMessage(@NotNull Player from, @NotNull String message) {
        for (Player target : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            var chatMessage = new ChatMessage(
                    Instant.now(),
                    runtime.hostname(),
                    //todo these should be using the players data id, not uuid
                    String.join("%s:%s", from.getUuid().toString(), target.getUuid().toString()),
                    from.getUuid().toString(),
                    message
            );

            try {
                storage.recordChatMessage(chatMessage);
                target.sendMessage("[STAFF] " + PlayerData.fromPlayer(from).getDisplayName() + ": " + message);
            } catch (Exception e) {
                logger.log(Level.ERROR, "Error sending staff chat message", e);
                from.sendMessage("Error sending staff chat message");
            }
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

        try {
            storage.recordChatMessage(message);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Error recording command", e);
        }
    }
}
