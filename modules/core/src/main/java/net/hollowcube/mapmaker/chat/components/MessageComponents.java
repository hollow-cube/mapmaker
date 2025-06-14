package net.hollowcube.mapmaker.chat.components;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.Emoji;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.temp.ChatMessageData;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MessageComponents {

    private static final TextColor PING_COLOR = TextColor.color(0xffe59e);
    private static final TextColor MAP_COLOR = TextColor.color(0x15ADD3);

    private static final Component HYPERCUBE_ONLY_EMOJI = Component.translatable("chat.emoji.no_hypercube");

    private final Cache<String, MapData> mapDataCache = Caffeine.newBuilder()
            .maximumSize(25)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    private final Cache<String, Component> usernameCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private final @NotNull MapService mapService;
    private final @NotNull PlayerService playerService;

    public MessageComponents(@NotNull MapService mapService, @NotNull PlayerService playerService) {
        this.mapService = mapService;
        this.playerService = playerService;
    }

    // region Component Parts

    @Blocking
    private void map(@NotNull MessageComponent.Builder builder, @NotNull String mapid, @NotNull Player player) {
        var uuid = player.getUuid().toString();
        var map = mapDataCache.get(mapid, id -> mapService.getMap(uuid, id));
        var author = usernameCache.get(map.owner(), id -> playerService.getPlayerDisplayName2(id).build());
        var progress = mapService.getMapProgress(uuid, List.of(mapid)).getProgress(mapid);

        var components = MapData.createHoverComponents(map, author, progress);

        var lore = components.getValue();
        lore.addAll(LanguageProviderV2.translateMulti("gui.play_maps.map_display_headless.footer", List.of()));

        var result = Component.text().append(components.getKey());
        for (var line : lore) {
            result = result.appendNewline().append(LanguageProviderV2.translate(line));
        }

        builder.append(
                Component.text(map.name(), MAP_COLOR)
                        .hoverEvent(HoverEvent.showText(result.build()))
                        .clickEvent(ClickEvent.runCommand("/play " + MapData.formatPublishedId(map.publishedId())))
        );
    }

    private void emoji(@NotNull MessageComponent.Builder builder, boolean hasHypercube, @NotNull String name, @NotNull Random random) {
        var emoji = Emoji.findByName(name);
        if (emoji == null) {
            builder.append(Component.text(":%s:".formatted(name)));
        } else if (!emoji.isPublic() && !hasHypercube && !emoji.isRandom()) {
            builder.appendError("hypercube_only", HYPERCUBE_ONLY_EMOJI);
            builder.append(Component.text(":%s:".formatted(name)));
        } else {
            // Random emojis can be used by anyone but only hypercube members can actually get the random emoji,
            // non-hypercube members will get a fixed emoji.
            builder.append(emoji.get(hasHypercube ? random : null));
        }
    }

    private void link(@NotNull MessageComponent.Builder builder, @NotNull String url) {
        url = url.replaceFirst("^https?://", "");

        builder.append(
                Component.text(url, NamedTextColor.BLUE)
                        .append(Component.text(FontUtil.computeOffset(2)))
                        .append(Component.text(BadSprite.require("icon/chat/external_link").fontChar(), NamedTextColor.BLUE))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open link")))
                        .clickEvent(ClickEvent.openUrl("https://%s".formatted(url)))
        );
    }

    // endregion

    public @NotNull MessageComponent createGlobalMessage(@NotNull Player player, @NotNull ChatMessageData message) {
        Random random = new Random(message.seed());

        var builder = MessageComponent.builder();

        for (var part : message.parts()) {
            switch (part.type()) {
                case RAW -> {
                    Component component = Component.text(part.text());

                    var namePattern = Pattern.compile(String.format("(?:^|\\s)(%s)", player.getUsername()), Pattern.CASE_INSENSITIVE);
                    if (namePattern.matcher(part.text()).find()) {
                        builder.ping(!player.getUuid().toString().equals(message.sender()));

                        component = component.replaceText(TextReplacementConfig.builder()
                                .match(namePattern)
                                .replacement((match, _) -> Component.text(match.group(), PING_COLOR))
                                .build());
                    }

                    builder.append(component);
                }
                case EMOJI -> this.emoji(builder, message.senderHasHypercube(), part.name(), random);
                case MAP -> this.map(builder, part.mapId(), player);
                case URL -> this.link(builder, part.text());
            }
        }

        return builder.build();
    }

    public @NotNull MessageComponent createDirectMessage(@NotNull Player player, @NotNull ChatMessageData message) {
        Random random = new Random(message.seed());

        var builder = MessageComponent.builder();

        for (var part : message.parts()) {
            switch (part.type()) {
                case RAW -> builder.append(Component.text(part.text()));
                case EMOJI -> this.emoji(builder, message.senderHasHypercube(), part.name(), random);
                case MAP -> this.map(builder, part.mapId(), player);
                case URL -> this.link(builder, part.text());
            }
        }

        return builder.build();
    }
}
