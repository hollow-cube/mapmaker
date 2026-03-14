package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractChatCommand extends CommandDsl {

    private final SessionManager sessions;
    private final MapService maps;
    private final ChatMessageListener messages;

    public AbstractChatCommand(
        SessionManager sessions, MapService maps, ChatMessageListener messages,
        String name, String... aliases
    ) {
        super(name, aliases);

        this.sessions = sessions;
        this.maps = maps;
        this.messages = messages;
    }

    protected void handle(Player player, String channel, String message) {
        String currentMapId = null;
        if (message.contains("[map]")) {
            var currentMap = MiscFunctionality.getCurrentMap(this.sessions, this.maps, player);
            if (currentMap == null || !currentMap.isPublished()) {
                player.sendMessage(Component.translatable("generic.map.chat.usage"));
                return;
            }
            currentMapId = currentMap.id();
        }

        var playerId = PlayerData.fromPlayer(player).id();
        long messageSeed = ThreadLocalRandom.current().nextLong();
        this.messages.trySendChatMessage(player, new ClientChatMessageData(
            ClientChatMessageData.Type.CHAT_UNSIGNED,
            playerId, message, channel,
            currentMapId, messageSeed
        ));
    }

}
