package net.hollowcube.posthog.flag;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record FeatureFlag(
        @NotNull String key,
        boolean isSimpleFlag,
        @NotNull Optional<Integer> rolloutPercentage,
        boolean active,
        @NotNull Filters filters,
        @NotNull Optional<Boolean> ensureExperienceContinuity
) {
    public static final Codec<FeatureFlag> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(FeatureFlag::key),
            Codec.BOOL.optionalFieldOf("is_simple_flag", false).forGetter(FeatureFlag::isSimpleFlag),
            Codec.INT.optionalFieldOf("rollout_percentage").forGetter(FeatureFlag::rolloutPercentage),
            Codec.BOOL.fieldOf("active").forGetter(FeatureFlag::active),
            Filters.CODEC.fieldOf("filters").forGetter(FeatureFlag::filters),
            Codec.BOOL.optionalFieldOf("ensure_experience_continuity").forGetter(FeatureFlag::ensureExperienceContinuity)
    ).apply(i, FeatureFlag::new));

    public record Filters(
            @NotNull Optional<Integer> aggregationGroupTypeIndex,
            @NotNull List<Condition> groups,
            @NotNull Optional<Variants> multivariate,
            @NotNull Map<String, String> payloads
    ) {
        public static final Codec<Filters> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("aggregation_group_type_index").forGetter(Filters::aggregationGroupTypeIndex),
                Codec.list(Condition.CODEC).fieldOf("groups").forGetter(Filters::groups),
                Variants.CODEC.optionalFieldOf("multivariate").forGetter(Filters::multivariate),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("payloads").forGetter(Filters::payloads)
        ).apply(i, Filters::new));
    }

    public record Variants(@NotNull List<Variant> variants) {
        public static final Codec<Variants> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.list(Variant.CODEC).optionalFieldOf("variants", List.of()).forGetter(Variants::variants)
        ).apply(i, Variants::new));
    }

    public record Variant(
            @NotNull String key,
            @NotNull String name,
            @NotNull Optional<Integer> rolloutPercentage
    ) {
        public static final Codec<Variant> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("key").forGetter(Variant::key),
                Codec.STRING.fieldOf("name").forGetter(Variant::name),
                Codec.INT.optionalFieldOf("rollout_percentage").forGetter(Variant::rolloutPercentage)
        ).apply(i, Variant::new));
    }

    public record Condition(
            @NotNull List<Property> properties,
            @NotNull Optional<Integer> rolloutPercentage,
            @NotNull Optional<String> variant
    ) {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.list(Property.CODEC).fieldOf("properties").forGetter(Condition::properties),
                Codec.INT.optionalFieldOf("rollout_percentage").forGetter(Condition::rolloutPercentage),
                Codec.STRING.optionalFieldOf("variant").forGetter(Condition::variant)
        ).apply(i, Condition::new));
    }

    public record Property(
            @NotNull String key,
            @NotNull Operator operator,
            @NotNull Either<List<Object>, Object> value, // Any primitive value
            @NotNull String type,
            boolean negation
    ) {
        public enum Operator {
            UNKNOWN,
            IS_SET, IS_NOT_SET,
            EXACT, IS_NOT,
            ICONTAINS, NOT_ICONTAINS,
            REGEX, NOT_REGEX,
            GT, LT, GTE, LTE;

            public static final Codec<Operator> CODEC = Codec.STRING.xmap(
                    name -> {
                        try {
                            return valueOf(name.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            return UNKNOWN;
                        }
                    },
                    value -> value.name().toLowerCase(Locale.ROOT));
        }

        public static final Codec<Property> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("key").forGetter(Property::key),
                Operator.CODEC.fieldOf("operator").forGetter(Property::operator),
                Codec.either(Codec.list(ExtraCodecs.ANY_PRIMITIVE), ExtraCodecs.ANY_PRIMITIVE)
                        .fieldOf("value").forGetter(Property::value),
                Codec.STRING.fieldOf("type").forGetter(Property::type),
                Codec.BOOL.optionalFieldOf("negation", false).forGetter(Property::negation)
        ).apply(i, Property::new));
    }
}
