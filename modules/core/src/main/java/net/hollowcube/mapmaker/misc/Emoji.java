package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Emoji(@NotNull String name, char raw, @NotNull Component component) {
    private static final Map<String, Emoji> EMOJI_MAP;
    private static final List<Category> EMOJIS_BY_CATEGORY;
    private static final PlayerInfoUpdatePacket EMOJI_PLAYERS_PACKET;

    public record Category(@NotNull String displayName, @NotNull List<Emoji> emojis) {}

    /**
     * Look up an emoji by its name (case-insensitive).
     *
     * @param name The name of the emoji, without the ':'s.
     * @return The emoji, or null if there is no such emoji.
     */
    public static @Nullable Emoji findByName(@NotNull String name) {
        return EMOJI_MAP.get(name.toLowerCase(Locale.ROOT));
    }

    public static @NotNull Collection<Category> categories() {
        return EMOJIS_BY_CATEGORY;
    }

    /**
     * Send the fake emoji players to the player for tab completions.
     *
     * <p>This is accomplished by sending an unlisted (on tab) player with each emoji name so that
     * when you press tab in chat it will show completions for them.</p>
     */
    public static void sendTabCompletions(@NotNull Player player) {
        //noinspection UnstableApiUsage
        player.sendPacket(EMOJI_PLAYERS_PACKET);
    }

    static {
        var categorySpriteMap = Map.of( // category -> name -> sprite name
                "symbols", Map.of(
                        "plus", "icon/plus",
                        "minus", "icon/minus",
                        "x", "icon/x_mark"
                ),
                "faces", Map.of(
                        "cool", "icon/emoji/cool",
                        "grin", "icon/emoji/grin",
                        "smile", "icon/emoji/smile",
                        "smirk", "icon/emoji/smirk",
                        "poop", "icon/emoji/poop"
                ),
                "misc", Map.of(
                        "crown", "icon/emoji/crown",
                        "grass", "icon/emoji/grass",
                        "sus", "icon/emoji/sus"
                )
        );
        var categoryNames = Map.of("symbols", "ѕʏᴍʙᴏʟѕ", "faces", "ꜰᴀᴄᴇѕ", "misc", "ᴍɪѕᴄ");

        EMOJI_MAP = categorySpriteMap.values().stream()
                .flatMap(map -> map.entrySet().stream())
                .map(entry -> {
                    var name = entry.getKey().toLowerCase(Locale.ROOT);
                    var sprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get(entry.getValue()), entry.getValue());
                    var component = Component.text(sprite.fontChar(), FontUtil.NO_SHADOW)
                            .hoverEvent(HoverEvent.showText(Component.text(":" + name + ":", NamedTextColor.WHITE)));
                    return new Emoji(name, sprite.fontChar(), component);
                })
                .collect(Collectors.toUnmodifiableMap(Emoji::name, Function.identity()));

        EMOJIS_BY_CATEGORY = categorySpriteMap.entrySet().stream()
                .map(entry -> {
                    var category = entry.getKey();
                    var displayName = categoryNames.get(category);
                    var emojis = entry.getValue().keySet().stream()
                            .map(EMOJI_MAP::get)
                            .toList();
                    return new Category(displayName, emojis);
                })
                .toList();

        // Build the player info packet
        var playerInfos = new ArrayList<PlayerInfoUpdatePacket.Entry>();
        for (var emoji : EMOJI_MAP.values()) {
            playerInfos.add(new PlayerInfoUpdatePacket.Entry(
                    UUID.randomUUID(), ":" + emoji.name + ":",
                    List.of(), false, 0, null, null, null
            ));
        }
        EMOJI_PLAYERS_PACKET = new PlayerInfoUpdatePacket(
                EnumSet.of(PlayerInfoUpdatePacket.Action.ADD_PLAYER, PlayerInfoUpdatePacket.Action.UPDATE_LISTED),
                playerInfos
        );
    }
}
