package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractChatCommand extends CommandDsl {

    private final SessionManager sessions;
    private final MapService maps;
    private final ChatMessageListener messages;


    public AbstractChatCommand(
            @NotNull SessionManager sessions, @NotNull MapService maps, @NotNull ChatMessageListener messages,
            @NotNull String name, @NotNull String... aliases
    ) {
        super(name, aliases);

        this.sessions = sessions;
        this.maps = maps;
        this.messages = messages;
    }

    protected void handle(
            @NotNull Player player,
            @NotNull String channel,
            @NotNull String message
    ) {
        message = FontUtil.stripInvalidChars(message).trim();
        if (message.isEmpty()) return;

        String currentMapId = null;
        if (message.contains("[map]")) {
            var currentMap = MiscFunctionality.getCurrentMap(this.sessions, this.maps, player);
            if (currentMap == null || !currentMap.isPublished()) {
                player.sendMessage(Component.translatable("generic.map.chat.usage"));
                return;
            }
            currentMapId = currentMap.id();
        }

        var playerId = PlayerDataV2.fromPlayer(player).id();
        long messageSeed = ThreadLocalRandom.current().nextLong();
        this.messages.trySendChatMessage(player, new ClientChatMessageData(
                ClientChatMessageData.Type.CHAT_UNSIGNED,
                playerId, message, channel,
                currentMapId, messageSeed
        ));
    }


}
