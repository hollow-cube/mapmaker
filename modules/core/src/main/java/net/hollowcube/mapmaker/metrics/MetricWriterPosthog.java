package net.hollowcube.mapmaker.metrics;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.hollowcube.posthog.PostHog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MetricWriterPosthog implements MetricWriter {
    private static final Logger logger = LoggerFactory.getLogger(MetricWriterPosthog.class);

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private static final String POSTHOG_API_KEY = "phc_mK0jji1aC3hvMBGLOLjuVARqolDGPS9AiuNUOhMwVyA"; // Not a secret
    private static final String POSTHOG_HOST = "https://us.i.posthog.com";
    public static final PostHog POSTHOG_CLIENT = new PostHog(POSTHOG_HOST, POSTHOG_API_KEY);

    public static final String NO_USER = "00000000-0000-0000-0000-000000000000";

    private final PostHog client;

    public MetricWriterPosthog() {
        client = POSTHOG_CLIENT;
    }

    @Override
    public void write(@NotNull Metric metric) {
        try {
            var name = computeMetricName(metric.getClass().getSimpleName());

            // We use gson here because its simple, it probably is pretty yikes performance wise.
            Map<String, Object> properties = new HashMap<>(GSON.fromJson(GSON.toJsonTree(metric), MAP_TYPE));

            String playerId = NO_USER;
            if (properties.containsKey("playerId")) {
                playerId = properties.get("playerId").toString();
                properties.remove("playerId");
            }

            client.capture(playerId, name, properties);
        } catch (Exception e) {
            logger.error("Failed to write metric", e);
        }
    }

    @Override
    public void close() {
        this.client.shutdown();
    }


    static @NotNull String computeMetricName(@NotNull String className) {
        return className.replace("Event", "")
                .replaceAll("([a-z])([A-Z]+)", "$1_$2")
                .toLowerCase(Locale.ROOT);
    }
}
