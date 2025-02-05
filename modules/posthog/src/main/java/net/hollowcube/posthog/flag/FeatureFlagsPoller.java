package net.hollowcube.posthog.flag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntPredicate;
import java.util.regex.PatternSyntaxException;

/**
 * Local feature flag fetcher and evaluator for PostHog.
 * <p>
 * Heavily based on <a href="https://github.com/PostHog/posthog-go/blob/master/featureflags.go#L342">this</a>.
 */
public class FeatureFlagsPoller {
    private static final int POLLING_INTERVAL_MS = 10 * 60 * 1000; // 10 minutes
    private static final String POSTHOG_ENDPOINT = "https://us.i.posthog.com";
    private static final String LOCAL_EVALUATION_ENDPOINT = "/api/feature_flag/local_evaluation";

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagsPoller.class);
    private static final Gson gson = new GsonBuilder().disableJdkUnsafe().create();

    private final String personalApiKey;

    private final HttpClient httpClient;
    private final Thread worker;

    private final Map<String, FeatureFlag> featureFlags = new ConcurrentHashMap<>();

    public FeatureFlagsPoller(@NotNull String personalApiKey) {
        this.personalApiKey = personalApiKey;

        this.httpClient = HttpClient.newHttpClient();
        this.worker = Thread.startVirtualThread(this::pollLoop);
    }

    public boolean getFeatureFlag(
            @NotNull String flagKey,
            @NotNull String distinctId,
            @NotNull Map<String, Object> properties
            // TODO: there is more API here.
    ) {
        // TODO variant support
        //  https://github.com/PostHog/posthog-go/blob/master/featureflags.go#L209

        try {
            var flag = this.featureFlags.get(flagKey);
            if (flag == null) return false;

            return computeFlagLocally(flag, distinctId, new HashMap<>(), properties, new HashMap<>(), new HashMap<>()) == EvaluationResult.TRUE;
        } catch (Throwable e) {
            logger.error("Error while evaluating feature flag", e);
            return false;
        }
    }

    public void close() {
        this.worker.interrupt();
    }

    private void pollLoop() {
        if (this.personalApiKey.isEmpty()) {
            logger.warn("Personal API key is empty, feature flags will not be fetched");
            return;
        }

        do {
            try {
                fetchLocalEvaluationFlags();
                logger.debug("(Re)loaded {} feature flags", this.featureFlags.size());
            } catch (InterruptedException ignored) {
                break;
            } catch (Throwable e) {
                logger.error("Error while polling feature flags", e);
            }

            LockSupport.parkNanos(POLLING_INTERVAL_MS * 1_000_000L);
        } while (!Thread.currentThread().isInterrupted());
    }

    @Blocking
    private void fetchLocalEvaluationFlags() throws IOException, InterruptedException {
        var endpoint = POSTHOG_ENDPOINT + LOCAL_EVALUATION_ENDPOINT;
        var request = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Authorization", "Bearer " + this.personalApiKey)
                .header("User-Agent", "github.com/hollow-cube/mapmaker")
                .build();

        var response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        var responseJson = gson.fromJson(response.body(), JsonElement.class);
        var responseFlags = FeatureFlagsResponse.CODEC.parse(JsonOps.INSTANCE, responseJson);
        var flags = responseFlags.getOrThrow();

        var newFlagMap = new HashMap<String, FeatureFlag>();
        for (var flag : flags.flags())
            newFlagMap.put(flag.key(), flag);
        this.featureFlags.clear();
        this.featureFlags.putAll(newFlagMap);

        //todo we also get 'group_type_mapping' and 'cohorts' but we arent really using them yet.
    }

    private @NotNull EvaluationResult computeFlagLocally(
            @NotNull FeatureFlag flag,
            @NotNull String distinctId,
            @NotNull Map<String, Object> groups,
            @NotNull Map<String, Object> properties,
            @NotNull Map<String, Map<String, Object>> groupProperties,
            @NotNull Map<String, PropertyGroup> cohorts
    ) {
        if (flag.ensureExperienceContinuity().isPresent() && flag.ensureExperienceContinuity().get()) {
            throw new IllegalArgumentException("Feature flag " + flag.key() + " requires experience continuity, cannot be evaluated locally");
        }

        if (!flag.active()) {
            return EvaluationResult.FALSE;
        }

        if (flag.filters().aggregationGroupTypeIndex().isPresent()) {
            // Throw
            // https://github.com/PostHog/posthog-go/blob/master/featureflags.go#L360
            throw new UnsupportedOperationException("Group eval not yet supported");
        }

        return matchFeatureFlagProperties(flag, distinctId, properties, cohorts);
    }

    private @NotNull EvaluationResult matchFeatureFlagProperties(
            @NotNull FeatureFlag flag,
            @NotNull String distinctId,
            @NotNull Map<String, Object> properties,
            @NotNull Map<String, PropertyGroup> cohorts
    ) {
        var isInconclusive = false;

        // Stable sort conditions with variant overrides to the top. This ensures that if overrides are present,
        // they are evaluated first, and the variant override is applied to the first matching condition.
        var sortedConditions = new ArrayList<>(flag.filters().groups());
        sortedConditions.sort((a, b) -> {
            int left = 1, right = 1;
            if (a.variant().isPresent())
                left = -1;
            if (b.variant().isPresent())
                right = -1;
            return left - right;
        });

        for (var condition : sortedConditions) {
            var result = isConditionMatch(flag, distinctId, condition, properties, cohorts);
            if (result == EvaluationResult.FALSE) continue;

            if (result == EvaluationResult.INCONCLUSIVE) {
                isInconclusive = true;
            } else if (result == EvaluationResult.TRUE) {
                var variantOverride = condition.variant();
                var multivariates = flag.filters().multivariate();

                if (variantOverride.isPresent() && multivariates.isPresent() && containsVariant(multivariates.get(), variantOverride.get())) {
                    // TODO: We are supposed to be returning the variant override name here.
                    //  https://github.com/PostHog/posthog-go/blob/master/featureflags.go#L462
                    return EvaluationResult.TRUE;
                } else {
                    return getMatchingVariant(flag, distinctId);
                }
            } else {
                throw new IllegalStateException("Unexpected evaluation result: " + result);
            }
        }

        if (isInconclusive) {
            return EvaluationResult.INCONCLUSIVE;
        }

        return EvaluationResult.FALSE;
    }

    private @NotNull EvaluationResult isConditionMatch(
            @NotNull FeatureFlag flag,
            @NotNull String distinctId,
            @NotNull FeatureFlag.Condition condition,
            @NotNull Map<String, Object> properties,
            @NotNull Map<String, PropertyGroup> cohorts
    ) {
        for (var prop : condition.properties()) {
            boolean isMatch = "cohort".equals(prop.type())
                    ? matchCohort(prop, properties, cohorts)
                    : matchProperty(prop, properties);
            if (!isMatch) {
                return EvaluationResult.FALSE;
            }
        }

        if (condition.rolloutPercentage().isPresent()) {
            return checkIfSimpleFlagEnabled(flag.key(), distinctId, condition.rolloutPercentage().get());
        }

        return EvaluationResult.TRUE;
    }

    private @NotNull EvaluationResult getMatchingVariant(@NotNull FeatureFlag flag, @NotNull String distinctId) {
        // TODO: variant support
        // https://github.com/PostHog/posthog-go/blob/master/featureflags.go#L381

        return EvaluationResult.TRUE;
    }

    private boolean containsVariant(@NotNull FeatureFlag.Variants variants, @NotNull String key) {
        for (var variant : variants.variants()) {
            if (variant.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private @NotNull EvaluationResult checkIfSimpleFlagEnabled(@NotNull String key, @NotNull String distinctId, int rolloutPercentage) {
        var value = hash(key, distinctId, "");
        return value <= rolloutPercentage / 100d ? EvaluationResult.TRUE : EvaluationResult.FALSE;
    }

    private boolean matchCohort(@NotNull FeatureFlag.Property prop, @NotNull Map<String, Object> properties, @NotNull Map<String, PropertyGroup> cohorts) {
        throw new UnsupportedOperationException("TODO: cohort support");
    }

    private boolean matchProperty(@NotNull FeatureFlag.Property prop, @NotNull Map<String, Object> properties) {
        var key = prop.key();
        var value = prop.value();

        var overrideValue = properties.get(key);
        if (overrideValue == null) {
            // TODO: most of the exceptions really are inconclusive results
            throw new IllegalStateException("Can't match properties without a given property value");
        }

        return switch (prop.operator()) {
            case IS_SET -> true; // Always a match because we checked that we have the property above.
            case IS_NOT_SET -> throw new IllegalStateException("Can't match properties with operator is_not_set");
            case EXACT -> {
                if (value.left().isPresent()) {
                    yield value.left().get().stream().anyMatch(overrideValue::equals);
                } else {
                    //noinspection OptionalGetWithoutIsPresent
                    yield overrideValue.equals(value.right().get());
                }
            }
            case IS_NOT -> {
                if (value.left().isPresent()) {
                    yield value.left().get().stream().noneMatch(overrideValue::equals);
                } else {
                    //noinspection OptionalGetWithoutIsPresent
                    yield !overrideValue.equals(value.right().get());
                }
            }
            case ICONTAINS -> ensureSingleString(value, comp -> overrideValue.toString()
                    .toLowerCase(Locale.ROOT).contains(comp.toLowerCase(Locale.ROOT)));
            case NOT_ICONTAINS -> !ensureSingleString(value, comp -> overrideValue.toString()
                    .toLowerCase(Locale.ROOT).contains(comp.toLowerCase(Locale.ROOT)));
            case REGEX -> ensureSingleString(value, regex -> {
                try {
                    return overrideValue.toString().matches(regex);
                } catch (PatternSyntaxException ignored) {
                    return false;
                }
            });
            case NOT_REGEX -> ensureSingleString(value, regex -> {
                try {
                    return !overrideValue.toString().matches(regex);
                } catch (PatternSyntaxException ignored) {
                    return false;
                }
            });
            case GT -> compareValues(value, overrideValue, i -> i > 0);
            case LT -> compareValues(value, overrideValue, i -> i < 0);
            case GTE -> compareValues(value, overrideValue, i -> i >= 0);
            case LTE -> compareValues(value, overrideValue, i -> i <= 0);

            // TODO: we kinda need to keep the key around here.
            case UNKNOWN -> throw new UnsupportedOperationException("Unknown operator received");
        };
    }

    public interface Func {
        boolean eval(String o);
    }

    private boolean ensureSingleString(
            @NotNull Either<List<Object>, Object> value,
            @NotNull Func func
    ) {
        if (value.right().isPresent())
            return func.eval(value.right().get().toString());
        return false;
    }

    private boolean compareValues(
            @NotNull Either<List<Object>, Object> value,
            @NotNull Object overrideValue,
            @NotNull IntPredicate predicate
    ) {
        if (value.right().isPresent()) {
            var comp = value.right().get();
            if (comp instanceof Comparable<?> && overrideValue instanceof Comparable<?> && comp.getClass() == overrideValue.getClass()) {
                //noinspection unchecked
                return predicate.test(((Comparable<Object>) comp).compareTo(overrideValue));
            }
        }
        return false;
    }

    // https://github.com/PostHog/posthog-go/blob/master/featureflags.go#L842
    private double hash(@NotNull String key, @NotNull String distinctId, @NotNull String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update((key + "." + distinctId + salt).getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            var hexString = sb.substring(0, Math.min(15, sb.length()));
            return ((double) Long.parseLong(hexString, 16)) / 0xfffffffffffffffL;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Unreachable
        }
    }

}