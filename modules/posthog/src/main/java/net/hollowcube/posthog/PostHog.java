package net.hollowcube.posthog;

import com.google.gson.*;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PostHog {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostHog.class);
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    private static final String DEFAULT_HOST = "https://app.posthog.com";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final QueueManager queueManager = new QueueManager(this::sendEvents, 50, Duration.ofSeconds(30));

    private final String host;
    private final String apiKey;

    public PostHog(@Nullable String host, @NotNull String apiKey) {
        this.host = Objects.requireNonNullElse(host, DEFAULT_HOST);
        this.apiKey = apiKey;
    }

    public void shutdown() {
        this.queueManager.shutdown();
    }

    private void enqueue(String distinctId, String event, Map<String, Object> properties) {
        JsonObject eventJson = getEventJson(event, distinctId, properties);
        queueManager.add(eventJson);
    }

    /**
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param event      name of the event. Must not be null or empty.
     * @param properties an array with any event properties you'd like to set.
     */
    public void capture(String distinctId, String event, Map<String, Object> properties) {
        enqueue(distinctId, event, properties);
    }

    /**
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param event      name of the event. Must not be null or empty.
     */
    public void capture(String distinctId, String event) {
        enqueue(distinctId, event, null);
    }

    /**
     * @param distinctId        which uniquely identifies your user in your
     *                          database. Must not be null or empty.
     * @param properties        an array with any person properties you'd like to
     *                          set.
     * @param propertiesSetOnce an array with any person properties you'd like to
     *                          set without overwriting previous values.
     */
    public void identify(String distinctId, Map<String, Object> properties, Map<String, Object> propertiesSetOnce) {
        Map<String, Object> props = new HashMap<String, Object>();
        if (properties != null) {
            props.put("$set", properties);
        }
        if (propertiesSetOnce != null) {
            props.put("$set_once", propertiesSetOnce);
        }
        enqueue(distinctId, "$identify", props);
    }

    /**
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     */
    public void identify(String distinctId, Map<String, Object> properties) {
        identify(distinctId, properties, null);
    }

    /**
     * @param distinctId distinct ID to merge. Must not be null or empty. Note: If
     *                   there is a conflict, the properties of this person will
     *                   take precedence.
     * @param alias      distinct ID to merge. Must not be null or empty. Note: If
     *                   there is a conflict, the properties of this person will be
     *                   overriden.
     */
    public void alias(String distinctId, String alias) {
        Map<String, Object> props = new HashMap<>();
        props.put("distinct_id", distinctId);
        props.put("alias", alias);
        enqueue(distinctId, "$create_alias", props);
    }

    /**
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     */
    public void set(String distinctId, Map<String, Object> properties) {
        Map<String, Object> props = new HashMap<>();
        props.put("$set", properties);
        enqueue(distinctId, "$set", props);
    }

    /**
     * @param distinctId which uniquely identifies your user in your database. Must
     *                   not be null or empty.
     * @param properties an array with any person properties you'd like to set.
     *                   Previous values will not be overwritten.
     */
    public void setOnce(String distinctId, Map<String, Object> properties) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("$set_once", properties);
        enqueue(distinctId, "$set_once", props);
    }

    private JsonObject getEventJson(String event, String distinctId, Map<String, Object> properties) {
        JsonObject eventJson = new JsonObject();
        // Ensure that we generate an identifier for this event such that we can e.g.
        // deduplicate server-side any duplicates we may receive.
        eventJson.addProperty("uuid", UUID.randomUUID().toString());
        eventJson.addProperty("timestamp", Instant.now().toString());
        eventJson.addProperty("distinct_id", distinctId);
        eventJson.addProperty("event", event);
        eventJson.addProperty("$lib", "posthog-java");
        if (properties != null) {
            eventJson.add("properties", GSON.toJsonTree(properties));
        }
        return eventJson;
    }

    /**
     * @param featureFlag which uniquely identifies your feature flag
     * @param distinctId  which uniquely identifies your user in your database. Must
     *                    not be null or empty.
     * @return whether the feature flag is enabled or not
     */
    public boolean isFeatureFlagEnabled(String featureFlag, String distinctId) {
        if (getFeatureFlags(distinctId).get(featureFlag) == null)
            return false;
        return Boolean.parseBoolean(getFeatureFlags(distinctId).get(featureFlag));
    }

    /**
     * @param featureFlag which uniquely identifies your feature flag
     * @param distinctId  which uniquely identifies your user in your database. Must
     *                    not be null or empty.
     * @return Variant of the feature flag
     */
    public String getFeatureFlag(String featureFlag, String distinctId) {
        return getFeatureFlags(distinctId).get(featureFlag);
    }

    /**
     * @param featureFlag which uniquely identifies your feature flag
     * @param distinctId  which uniquely identifies your user in your database. Must
     *                    not be null or empty.
     * @return The feature flag payload, if it exists
     */
    public String getFeatureFlagPayload(String featureFlag, String distinctId) {
        return getFeatureFlagPayloads(distinctId).get(featureFlag);
    }

    private Map<String, String> getFeatureFlags(String distinctId) {
        JsonObject response = decide("/decide/?v=3", distinctId);

        HashMap<String, String> featureFlags = new HashMap<>();

        JsonObject flags = response.getAsJsonObject("featureFlags");
        for (String flag : flags.keySet()) {
            featureFlags.put(flag, flags.get(flag).toString());
        }

        return featureFlags;
    }

    private Map<String, String> getFeatureFlagPayloads(String distinctId) {
        JsonObject response = decide("/decide/?v=3", distinctId);

        HashMap<String, String> flagPayloads = new HashMap<>();

        JsonObject payloads = response.getAsJsonObject("featureFlagPayloads");
        for (String flag : payloads.keySet()) {
            String payload = payloads.get(flag).toString();
            flagPayloads.put(flag, payload);
        }

        return flagPayloads;
    }

    @Blocking
    private void sendEvents(@NotNull JsonArray events) {
        if (events.isEmpty()) return;

        JsonObject body = new JsonObject();
        body.addProperty("api_key", apiKey);
        body.add("batch", events);

        var req = HttpRequest.newBuilder(URI.create(host + "/batch"))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
        try {
            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (!(res.statusCode() > 199 && res.statusCode() < 300)) {
                LOGGER.error("Error sending events: " + res.body());
            }
        } catch (IOException e) {
            LOGGER.error("Error sending events", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Blocking
    private JsonObject decide(@NotNull String path, @NotNull String distinctId) {
        JsonObject bodyJSON = new JsonObject();
        bodyJSON.addProperty("api_key", apiKey);
        bodyJSON.addProperty("distinct_id", distinctId);

        var req = HttpRequest.newBuilder(URI.create(host + path))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(bodyJSON)))
                .build();
        try {
            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() > 199 && res.statusCode() < 300) {
                return GSON.fromJson(res.body(), JsonObject.class);
            }

            LOGGER.error("Error calling API: " + res.body());
        } catch (IOException e) {
            LOGGER.error("Error calling API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }
}
