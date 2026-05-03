package net.hollowcube.mapmaker.chat.components;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.Emoji;
import net.hollowcube.mapmaker.player.PlayerData;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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

    private final @NotNull ApiClient api;
    private final @NotNull MapService mapService;
    private final @NotNull PlayerService playerService;

    public MessageComponents(@NotNull ApiClient api, @NotNull MapService mapService, @NotNull PlayerService playerService) {
        this.api = api;
        this.mapService = mapService;
        this.playerService = playerService;
    }

    // region Component Parts

    @Blocking
    private void map(@NotNull MessageComponent.Builder builder, @NotNull String mapId, @NotNull Player player) {
        var uuid = player.getUuid().toString();
        var map = mapDataCache.get(mapId, api.maps::get);
        var author = usernameCache.get(map.owner(), id -> playerService.getPlayerDisplayName2(id).build());
        var progress = api.maps.searchMapProgress(uuid, List.of(mapId)).first();

        var playerProtocolVersion = ProtocolVersions.getProtocolVersion(player);
        var components = MapData.createHoverComponents(map, author, progress, playerProtocolVersion);

        var lore = components.getValue();
        if (playerProtocolVersion >= map.protocolVersion()) {
            lore.addAll(LanguageProviderV2.translateMulti("gui.play_maps.map_display_headless.footer", List.of()));
        }

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
        url = "https://%s".formatted(url.replaceFirst("^https?://", ""));

        try {
            var uri = new URI(url);

            builder.append(
                Component.text(url, NamedTextColor.BLUE)
                    .append(Component.text(FontUtil.computeOffset(2)))
                    .append(Component.text(BadSprite.require("icon/chat/external_link").fontChar(), NamedTextColor.BLUE))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to open link")))
                    .clickEvent(ClickEvent.openUrl(uri.toString()))
            );
        } catch (URISyntaxException e) {
            builder.append(Component.text(url));
        }
    }

    // endregion

    public @NotNull MessageComponent createGlobalMessage(@NotNull Player player, @NotNull ChatMessageData message) {
        Random random = new Random(message.seed());
        var shouldUwuify = PlayerData.fromPlayer(player).getSetting(PlayerSettings.CHAT_LANGUAGE) == ChatLanguage.UWU;

        var builder = MessageComponent.builder();

        for (var part : message.parts()) {
            switch (part.type()) {
                case RAW -> {
                    Component component = Component.text(shouldUwuify ? uwuify(part.text(), random) : part.text());

                    var namePattern = Pattern.compile(String.format("(?:^|\\s)(%s)", player.getUsername()), Pattern.CASE_INSENSITIVE);
                    if (namePattern.matcher(part.text()).find()) {
                        builder.ping(!player.getUuid().toString().equals(message.sender()));

                        if (!shouldUwuify) {
                            component = component.replaceText(
                                TextReplacementConfig.builder()
                                    .match(namePattern)
                                    .replacement((match, _) -> Component.text(match.group(), PING_COLOR)).build()
                            );
                        }
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
        var shouldUwuify = PlayerData.fromPlayer(player).getSetting(PlayerSettings.CHAT_LANGUAGE) == ChatLanguage.UWU;

        var builder = MessageComponent.builder();

        for (var part : message.parts()) {
            switch (part.type()) {
                case RAW  -> builder.append(Component.text(shouldUwuify ? uwuify(part.text(), random) : part.text()));
                case EMOJI -> this.emoji(builder, message.senderHasHypercube(), part.name(), random);
                case MAP -> this.map(builder, part.mapId(), player);
                case URL -> this.link(builder, part.text());
            }
        }

        return builder.build();
    }

    private static String uwuify(String input, Random random) {
        var parts = input.split(" ");
        var output = new ArrayList<String>();
        var index = 0;
        for (var part : parts) {
            if (part.startsWith("@")) {
                output.add(part);
            } else {
                var uwuified = part;
                if (random.nextInt(5) == 0 && part.length() >= 3) {
                    var character = uwuified.charAt(0);
                    var stutterCount = random.nextInt(1) + 1;
                    for (var i = 0; i < stutterCount; i++) {
                        uwuified = "%s-%s".formatted(character, uwuified);
                    }
                }

                uwuified = uwuified.replaceAll("[rl]", "w");
                uwuified = uwuified.replaceAll("[RL]", "W");
                uwuified = uwuified.replaceAll("n([aeiou])", "ny$1");
                uwuified = uwuified.replaceAll("N([aeiouAEIOU])", "Ny$1");
                uwuified = uwuified.replaceAll("(?i)ove", "uv");

                if (uwuified.endsWith("!") || uwuified.endsWith("?") || uwuified.endsWith(".")) {
                    if (random.nextInt(4) == 0) {
                        var extraPunctuationCount = random.nextInt(3) + 1;
                        var extraPunctuation = new StringBuilder();
                        for (var i = 0; i < extraPunctuationCount; i++) {
                            extraPunctuation.append(random.nextBoolean() ? "!" : "?");
                        }
                        uwuified += extraPunctuation;
                    }
                }

                output.add(uwuified);
            }

            if (index < parts.length - 1 && parts.length > 5 && random.nextInt(10) == 0) {
                if (random.nextInt(10) == 0) {
                    output.add("owo");
                } else if (random.nextInt(10) == 0) {
                    output.add("uwu");
                } else if (random.nextInt(10) == 0) {
                    output.add("rawr");
                } else if (random.nextInt(10) == 0) {
                    output.add("nya~");
                }
            }
            index++;
        }

        return String.join(" ", output);
    }
}
