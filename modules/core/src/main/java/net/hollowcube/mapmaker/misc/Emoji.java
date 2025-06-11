package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.util.Either;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.function.Function;

public record Emoji(
        @NotNull String name,
        boolean showInHelp,
        boolean isPublic,
        @NotNull Either<Component, Function<Random, Component>> component
) {
    private static final LinkedHashMap<String, Emoji> EMOJI_MAP = new LinkedHashMap<>();

    public static final Emoji SMILE = builder("smile").parent("face").build();
    public static final Emoji GLAD = builder("glad").parent("face").hypercube().build();
    public static final Emoji NERD = builder("nerd").parent("face").hypercube().build();
    public static final Emoji JOY = builder("joy").parent("face").build();
    public static final Emoji INNOCENT = builder("innocent").parent("face").hypercube().build();
    public static final Emoji COOL = builder("cool").parent("face").hypercube().build();
    public static final Emoji RAISED_EYEBROW = builder("raised_eyebrow").parent("face").build();
    public static final Emoji NEUTRAL = builder("neutral").parent("face").hypercube().build();
    public static final Emoji SALUTE = builder("salute").parent("face").build();
    public static final Emoji PLEADING = builder("pleading").parent("face").build();
    public static final Emoji FLUSHED = builder("flushed").parent("face").build();
    public static final Emoji THINKING = builder("thinking").parent("face").build();
    public static final Emoji CONFUSED = builder("confused").parent("face").build();
    public static final Emoji SLEEPING = builder("sleeping").parent("face").build();
    public static final Emoji ROLLING_EYES = builder("rolling_eyes").parent("face").hypercube().build();
    public static final Emoji SOB = builder("sob").parent("face").build();
    public static final Emoji SCREAM = builder("scream").parent("face").build();
    public static final Emoji HOT = builder("hot").parent("face").hypercube().build();
    public static final Emoji COLD = builder("cold").parent("face").hypercube().build();
    public static final Emoji CLOWN = builder("clown").parent("face").build();
    public static final Emoji ANGRY = builder("angry").parent("face").build();
    public static final Emoji VOMIT = builder("vomit").parent("face").hypercube().build();
    public static final Emoji POOP = builder("poop").parent("misc").build();
    public static final Emoji DEVIL = builder("devil").parent("face").build();
    public static final Emoji ALIEN = builder("alien").parent("face").build();
    public static final Emoji BYEBYE = builder("byebye").parent("face").build();
    public static final Emoji DESOLATE = builder("desolate").parent("face").build();
    public static final Emoji HEARTEYES = builder("hearteyes").parent("face").build();

    public static final Emoji THUMBS_UP = builder("thumbs_up").parent("misc").build(); //todo alias
    public static final Emoji THUMBS_DOWN = builder("thumbs_down").parent("misc").build();
    public static final Emoji PRAY = builder("pray").parent("misc").build();
    public static final Emoji OK = builder("ok").parent("misc").hypercube().build();
    public static final Emoji CROWN = builder("crown").parent("misc").hypercube().build();
    public static final Emoji L = builder("l").parent("misc").build();
    public static final Emoji GG = builder("gg").parent("misc").build();
    public static final Emoji _100 = builder("100").parent("misc").hypercube().build();
    public static final Emoji TADA = builder("tada").parent("misc").hypercube().build();
    public static final Emoji FIRE = builder("fire").parent("misc").build();
    public static final Emoji HEART = builder("heart").parent("misc").build();

    public static final Emoji EGGPLANT = builder("eggplant").parent("misc").hypercube().build();
    public static final Emoji MOYAI = builder("moyai").parent("misc").hypercube().build();
    public static final Emoji SKULL = builder("skull").parent("face").build();
    public static final Emoji BEANS = builder("beans").parent("misc").hypercube().build();

    public static final Emoji GOLD_BLOB = builder("goldblob").parent("itmg").build();
    public static final Emoji SEAL = builder("seal").parent("itmg").build();
    public static final Emoji ITMG = builder("itmg").path("itmg/blob").hideInHelp().build();
    public static final Emoji BLOB = builder("blob").parent("itmg").alternative(GOLD_BLOB, 0.01f).build();

    public static final Emoji GRASS = builder("grass").parent("misc").hypercube().build();
    public static final Emoji MUSHROOM = builder("mushroom").parent("misc").hypercube().build();
    public static final Emoji ROCKET = builder("rocket").parent("misc").build();
    public static final Emoji COLON_3 = builder("3").parent("misc").build();
    public static final Emoji BRAIN = builder("brain").parent("misc").build();
    public static final Emoji EYE = builder("eye").parent("misc").build();
    public static final Emoji GOAT = builder("goat").parent("misc").build();
    public static final Emoji MOUTH = builder("mouth").parent("misc").build();
    public static final Emoji PEPODUMB = builder("pepodumb").parent("misc").hypercube().build();

    public static final Emoji SUS_BLUE = builder("sus_blue").path("sus/blue").hideInHelp().hypercube().build();
    public static final Emoji SUS_BROWN = builder("sus_brown").path("sus/brown").hideInHelp().hypercube().build();
    public static final Emoji SUS_CYAN = builder("sus_cyan").path("sus/cyan").hideInHelp().hypercube().build();
    public static final Emoji SUS_GRAY = builder("sus_gray").path("sus/gray").hideInHelp().hypercube().build();
    public static final Emoji SUS_GREEN = builder("sus_green").path("sus/green").hideInHelp().hypercube().build();
    public static final Emoji SUS_LIME = builder("sus_lime").path("sus/lime").hideInHelp().hypercube().build();
    public static final Emoji SUS_ORANGE = builder("sus_orange").path("sus/orange").hideInHelp().hypercube().build();
    public static final Emoji SUS_PINK = builder("sus_pink").path("sus/pink").hideInHelp().hypercube().build();
    public static final Emoji SUS_PURPLE = builder("sus_purple").path("sus/purple").hideInHelp().hypercube().build();
    public static final Emoji SUS_RED = builder("sus_red").path("sus/red").hypercube().build(); // Default color, not hidden in help
    public static final Emoji SUS_WHITE = builder("sus_white").path("sus/white").hideInHelp().hypercube().build();
    public static final Emoji SUS_YELLOW = builder("sus_yellow").path("sus/yellow").hideInHelp().hypercube().build();
    public static final Emoji SUS = builder("sus").hideInHelp().random(
            SUS_BLUE, SUS_BROWN, SUS_CYAN, SUS_GRAY, SUS_GREEN, SUS_LIME, SUS_ORANGE,
            SUS_PINK, SUS_PURPLE, SUS_RED, SUS_WHITE, SUS_YELLOW
    ).build();

    public static @Nullable Emoji findByName(@NotNull String name) {
        return EMOJI_MAP.get(name.toLowerCase());
    }

    public static @NotNull Collection<Emoji> values() {
        return EMOJI_MAP.sequencedValues();
    }

    // Accessors
    public boolean isRandom() {
        return this.component.isRight();
    }

    public Component get(@Nullable Random random) {
        return this.component.map(Function.identity(), func -> func.apply(random));
    }

    // Construction logic

    private static @NotNull Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    private static class Builder {
        private final String id;
        private Either<Component, Function<Random, Component>> component = null;
        private boolean showInHelp = true;
        private boolean isPublic = true;

        private Builder(@NotNull String id) {
            this.id = id;
        }

        public Builder parent(@NotNull String path) {
            return path(path + "/" + this.id);
        }

        public Builder path(@NotNull String fullPath) {
            var sprite = BadSprite.require("icon/emoji/" + fullPath);
            var component = Component.text(sprite.fontChar(), NamedTextColor.WHITE)
                    .shadowColor(ShadowColor.none())
                    .hoverEvent(HoverEvent.showText(Component.text(":" + this.id + ":", NamedTextColor.WHITE)));
            this.component = Either.left(component);
            return this;
        }

        public Builder random(@NotNull Emoji... choices) {
            this.component = Either.right(random -> {
                if (random == null) return SUS_RED.get(null);
                return choices[random.nextInt(choices.length)].get(random);
            });
            return this;
        }

        public Builder alternative(@NotNull Emoji alternative, float chance) {
            var common = this.component;
            this.component = Either.right(random -> {
                if (random == null) return common.map(Function.identity(), func -> func.apply(null));
                return random.nextFloat() > chance ? alternative.get(random) : common.map(Function.identity(), func -> func.apply(random));
            });
            return this;
        }

        public Builder hideInHelp() {
            this.showInHelp = false;
            return this;
        }

        public Builder hypercube() {
            this.isPublic = false;
            return this;
        }

        public Emoji build() {
            Check.argCondition(EMOJI_MAP.containsKey(this.id), "Duplicate emoji: " + this.id);
            Check.stateCondition(this.component == null, "No sprite set for emoji: " + this.id);
            var emoji = new Emoji(this.id, this.showInHelp, this.isPublic, this.component);
            EMOJI_MAP.put(this.id, emoji);
            return emoji;
        }
    }

}
