package net.hollowcube.mapmaker.chat;

import net.hollowcube.mapmaker.misc.Emoji;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.CustomChatCompletionPacket;

import java.util.ArrayList;
import java.util.List;

public class ChatAutoCompleter {

    private static final List<String> CONSTANT_COMPLETIONS = List.of(
        "[map]"
    );

    public static void sendSuggestions(Player player) {
        List<String> completions = new ArrayList<>(CONSTANT_COMPLETIONS);

        // TODO in the future may be can make it so that it only sends the emojis that the player has access to.
        for (Emoji value : Emoji.values()) {
            completions.add(":" + value.name() + ":");
        }

        player.sendPacket(new CustomChatCompletionPacket(CustomChatCompletionPacket.Action.ADD, completions));
    }
}
