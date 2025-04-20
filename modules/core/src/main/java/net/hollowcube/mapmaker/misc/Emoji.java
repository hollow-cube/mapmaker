package net.hollowcube.mapmaker.misc;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public record Emoji(
        @NotNull String name,
        boolean showInHelp,
        @NotNull Function<Random, Component> supplier
) {
    private static final LinkedHashMap<String, Emoji> EMOJI_MAP = new LinkedHashMap<>();
    private static final Set<String> PUBLIC_EMOJIS = Set.of(new String[]{
            // MUST BE KEPT IN SYNC WITH SESSION SERVICE
            "smile", "joy", "raised_eyebrow", "salute", "pleading",
            "flushed", "thinking", "confused", "sleeping", "sob",
            "scream", "clown", "angry", "poop", "devil", "thumbs_up",
            "thumbs_down", "pray", "l", "gg", "fire", "heart",
            "skull", "blob", "seal", "itmg", "rocket",
            // MUST BE KEPT IN SYNC WITH SESSION SERVICE
    });

    public static @Nullable Emoji findByName(@NotNull String name) {
        return EMOJI_MAP.get(name.toLowerCase());
    }

    public static boolean isPublic(@NotNull Emoji emoji) {
        return PUBLIC_EMOJIS.contains(emoji.name);
    }

    public static @NotNull Collection<Emoji> values() {
        return EMOJI_MAP.sequencedValues();
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

    public static final Emoji THUMBS_UP = builder("thumbs_up").parent("misc").build(); //todo alias
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
    ).build();

    // Construction logic

    private static @NotNull Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    private static class Builder {
        private final String id;
        private Function<Random, Component> supplier = null;
        private boolean showInHelp = true;

        private Builder(@NotNull String id) {
            this.id = id;
        }

        public Builder parent(@NotNull String path) {
            return path(path + "/" + id);
        }

        public Builder path(@NotNull String fullPath) {
            var sprite = BadSprite.require("icon/emoji/" + fullPath);
            var component = Component.text(sprite.fontChar())
                    .shadowColor(ShadowColor.none())
                    .hoverEvent(HoverEvent.showText(Component.text(":" + id + ":", NamedTextColor.WHITE)));
            supplier = $ -> component;
            return this;
        }

        public Builder random(@NotNull Emoji... choices) {
            supplier = random -> {
                if (random == null) return SUS_RED.supplier.apply(null);
                return choices[random.nextInt(choices.length)].supplier.apply(random);
            };
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
            return emoji;
        }
    }

}
