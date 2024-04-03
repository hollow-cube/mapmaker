package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public record Emoji(
        @NotNull String name,
        boolean showInHelp,
        @NotNull Supplier<Component> supplier
) {
    private static final List<PlayerInfoUpdatePacket.Entry> EMOJI_PACKET_ENTRIES = new ArrayList<>();
    private static final LinkedHashMap<String, Emoji> EMOJI_MAP = new LinkedHashMap<>();

    public static @Nullable Emoji findByName(@NotNull String name) {
        return EMOJI_MAP.get(name.toLowerCase());
    }

    public static @NotNull Collection<Emoji> values() {
        return EMOJI_MAP.sequencedValues();
    }

    /**
     * Send the fake emoji players to the player for tab completions.
     *
     * <p>This is accomplished by sending an unlisted (on tab) player with each emoji name so that
     * when you press tab in chat it will show completions for them.</p>
     */
    public static void sendTabCompletions(@NotNull Player player) {
        //noinspection UnstableApiUsage
        player.sendPacket(new PlayerInfoUpdatePacket(
                EnumSet.of(PlayerInfoUpdatePacket.Action.ADD_PLAYER, PlayerInfoUpdatePacket.Action.UPDATE_LISTED),
                EMOJI_PACKET_ENTRIES
        ));
    }

    public static final Emoji SMILE = builder("smile").parent("face").build();
    public static final Emoji GLAD = builder("glad").parent("face").build();
    public static final Emoji NERD = builder("nerd").parent("face").build();
    public static final Emoji JOY = builder("joy").parent("face").build();
    public static final Emoji INNOCENT = builder("innocent").parent("face").build();
    public static final Emoji COOL = builder("cool").parent("face").build();
    public static final Emoji RAISED_EYEBROW = builder("raised_eyebrow").parent("face").build();
    public static final Emoji NEUTRAL = builder("neutral").parent("face").build();
    public static final Emoji SALUTE = builder("salute").parent("face").build();
    public static final Emoji PLEADING = builder("pleading").parent("face").build();
    public static final Emoji FLUSHED = builder("flushed").parent("face").build();
    public static final Emoji THINKING = builder("thinking").parent("face").build();
    public static final Emoji CONFUSED = builder("confused").parent("face").build();
    public static final Emoji SLEEPING = builder("sleeping").parent("face").build();
    public static final Emoji ROLLING_EYES = builder("rolling_eyes").parent("face").build();
    public static final Emoji SOB = builder("sob").parent("face").build();
    public static final Emoji SCREAM = builder("scream").parent("face").build();
    public static final Emoji HOT = builder("hot").parent("face").build();
    public static final Emoji COLD = builder("cold").parent("face").build();
    public static final Emoji CLOWN = builder("clown").parent("face").build();
    public static final Emoji ANGRY = builder("angry").parent("face").build();
    public static final Emoji VOMIT = builder("vomit").parent("face").build();
    public static final Emoji POOP = builder("poop").parent("misc").build();
    public static final Emoji DEVIL = builder("devil").parent("face").build();

    public static final Emoji THUMBS_UP = builder("thumbs_up").parent("misc").build();
    public static final Emoji THUMBS_DOWN = builder("thumbs_down").parent("misc").build();
    public static final Emoji PRAY = builder("pray").parent("misc").build();
    public static final Emoji OK = builder("ok").parent("misc").build();
    public static final Emoji CROWN = builder("crown").parent("misc").build();
    public static final Emoji L = builder("l").parent("misc").build();
    public static final Emoji GG = builder("gg").parent("misc").build();
    public static final Emoji _100 = builder("100").parent("misc").build();
    public static final Emoji TADA = builder("tada").parent("misc").build();
    public static final Emoji FIRE = builder("fire").parent("misc").build();
    public static final Emoji HEART = builder("heart").parent("misc").build();

    public static final Emoji EGGPLANT = builder("eggplant").parent("misc").build();
    public static final Emoji MOYAI = builder("moyai").parent("misc").build();
    public static final Emoji SKULL = builder("skull").parent("face").build();
    public static final Emoji BEANS = builder("beans").parent("misc").build();

    public static final Emoji BLOB = builder("blob").parent("itmg").build();
    public static final Emoji SEAL = builder("seal").parent("itmg").build();
    public static final Emoji ITMG = builder("itmg").path("itmg/blob").hideInHelp().build();

    public static final Emoji GRASS = builder("grass").parent("misc").build();
    public static final Emoji MUSHROOM = builder("mushroom").parent("misc").build();
    public static final Emoji ROCKET = builder("rocket").parent("misc").build();

    public static final Emoji SUS_BLUE = builder("sus_blue").path("sus/blue").hideInHelp().build();
    public static final Emoji SUS_BROWN = builder("sus_brown").path("sus/brown").hideInHelp().build();
    public static final Emoji SUS_CYAN = builder("sus_cyan").path("sus/cyan").hideInHelp().build();
    public static final Emoji SUS_GRAY = builder("sus_gray").path("sus/gray").hideInHelp().build();
    public static final Emoji SUS_GREEN = builder("sus_green").path("sus/green").hideInHelp().build();
    public static final Emoji SUS_LIME = builder("sus_lime").path("sus/lime").hideInHelp().build();
    public static final Emoji SUS_ORANGE = builder("sus_orange").path("sus/orange").hideInHelp().build();
    public static final Emoji SUS_PINK = builder("sus_pink").path("sus/pink").hideInHelp().build();
    public static final Emoji SUS_PURPLE = builder("sus_purple").path("sus/purple").hideInHelp().build();
    public static final Emoji SUS_RED = builder("sus_red").path("sus/red").hideInHelp().build();
    public static final Emoji SUS_WHITE = builder("sus_white").path("sus/white").hideInHelp().build();
    public static final Emoji SUS_YELLOW = builder("sus_yellow").path("sus/yellow").hideInHelp().build();
    public static final Emoji SUS = builder("sus").random(
            SUS_BLUE, SUS_BROWN, SUS_CYAN, SUS_GRAY, SUS_GREEN, SUS_LIME, SUS_ORANGE,
            SUS_PINK, SUS_PURPLE, SUS_RED, SUS_WHITE, SUS_YELLOW
    ).hideInHelp().build();

    // Construction logic

    private static @NotNull Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    private static class Builder {
        private final String id;
        private Supplier<Component> supplier = null;
        private boolean showInHelp = true;

        private Builder(@NotNull String id) {
            this.id = id;
        }

        public Builder parent(@NotNull String path) {
            return path(path + "/" + id);
        }

        public Builder path(@NotNull String fullPath) {
            var sprite = BadSprite.require("icon/emoji/" + fullPath);
            var component = Component.text(sprite.fontChar(), FontUtil.NO_SHADOW)
                    .hoverEvent(HoverEvent.showText(Component.text(":" + id + ":", NamedTextColor.WHITE)));
            supplier = () -> component;
            return this;
        }

        public Builder random(@NotNull Emoji... choices) {
            supplier = () -> choices[ThreadLocalRandom.current().nextInt(choices.length)].supplier.get();
            return this;

        }

        public Builder hideInHelp() {
            showInHelp = false;
            return this;
        }

        public Emoji build() {
            Check.argCondition(EMOJI_MAP.containsKey(id), "Duplicate emoji: " + id);
            Check.stateCondition(supplier == null, "No sprite set for emoji: " + id);
            var emoji = new Emoji(id, showInHelp, supplier);
            EMOJI_MAP.put(id, emoji);
            EMOJI_PACKET_ENTRIES.add(new PlayerInfoUpdatePacket.Entry(
                    UUID.randomUUID(), ":" + id + ":",
                    List.of(), false, 0, null, null, null
            ));
            return emoji;
        }
    }

}
