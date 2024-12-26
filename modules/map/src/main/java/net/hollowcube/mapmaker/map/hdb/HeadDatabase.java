package net.hollowcube.mapmaker.map.hdb;

import io.opentelemetry.api.OpenTelemetry;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HeadDatabase extends AbstractHttpService {
    private static final String BASE_URL = "https://headdatabase.hollow-cube.workers.dev";
    // This is kind of a secret and maybe we should not hardcode it. It allows you to query that cloudflare worker
    // which shouldnt be public but also theres no secret info or anything i just dont want it to become a public
    // api for people to use.
    // In the future I will perhaps put it behind CF access or something.
    private static final String AUTH_TOKEN = "hYg4Vj6NWSe3FKMLDpXfbAmGvntPdR5qu9TwEkHyazJxc27Q8r";

    private static final Map<String, ItemStack> CATEGORY_ICONS = Map.ofEntries(
                    Map.entry("alphabet", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTY3ZDgxM2FlN2ZmZTViZTk1MWE0ZjQxZjJhYTYxOWE1ZTM4OTRlODVlYTVkNDk4NmY4NDk0OWM2M2Q3NjcyZSJ9fX0="),
                    Map.entry("animals", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWQ2YzZlZGE5NDJmN2Y1ZjcxYzMxNjFjNzMwNmY0YWVkMzA3ZDgyODk1ZjlkMmIwN2FiNDUyNTcxOGVkYzUifX19"),
                    Map.entry("blocks", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTFlZDlhYmY1MWZlNGVhODRjZmNiMjcyOTdmMWJjNTRjZDM4MmVkZjg1ZTdiZDZlNzVlY2NhMmI4MDY2MTEifX19"),
                    Map.entry("decoration", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2UyMjM5MWUzNWEzZTViY2VlODlkYjMxMmU4NzRmZGM5ZDllN2E2MzUxMzE0YjgyYmRhOTdmYmQyYmU4N2ViOCJ9fX0="),
                    Map.entry("food-drinks", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTE5OTdkYTY0MDQzYjI4NDgyMjExNTY0M2E2NTRmZGM0ZThhNzIyNjY2NGI0OGE0ZTFkYmI1NTdiNWMwZmUxNCJ9fX0="),
                    Map.entry("humans", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWI3YWY5ZTQ0MTEyMTdjN2RlOWM2MGFjYmQzYzNmZDY1MTk3ODMzMzJhMWIzYmM1NmZiZmNlOTA3MjFlZjM1In19fQ=="),
                    Map.entry("humanoid", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODIyZDhlNzUxYzhmMmZkNGM4OTQyYzQ0YmRiMmY1Y2E0ZDhhZThlNTc1ZWQzZWIzNGMxOGE4NmU5M2IifX19"),
                    Map.entry("miscellaneous", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTJlOTgxNjVkZWVmNGVkNjIxOTUzOTIxYzFlZjgxN2RjNjM4YWY3MWMxOTM0YTQyODdiNjlkN2EzMWY2YjgifX19"),
                    Map.entry("monsters", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTZmYzg1NGJiODRjZjRiNzY5NzI5Nzk3M2UwMmI3OWJjMTA2OTg0NjBiNTFhNjM5YzYwZTVlNDE3NzM0ZTExIn19fQ=="),
                    Map.entry("plants", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2JiMzExZjNiYTFjMDdjM2QxMTQ3Y2QyMTBkODFmZTExZmQ4YWU5ZTNkYjIxMmEwZmE3NDg5NDZjMzYzMyJ9fX0=")
            ).entrySet().stream()
            .map(e -> Map.entry(e.getKey(), ItemStack.builder(Material.PLAYER_HEAD)
                    .set(ItemComponent.PROFILE, new HeadProfile(new PlayerSkin(e.getValue(), null)))
                    .build()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private final String authToken;

    public HeadDatabase(@NotNull OpenTelemetry otel) {
        this(otel, AUTH_TOKEN);
    }

    public HeadDatabase(@NotNull OpenTelemetry otel, @NotNull String authToken) {
        super(otel);
        this.authToken = authToken;
    }

    public @NotNull Collection<String> categories() {
        return CATEGORY_ICONS.keySet();
    }

    public @NotNull ItemStack categoryIcon(@NotNull String category) {
        return CATEGORY_ICONS.getOrDefault(category, ItemStack.of(Material.BARRIER));
    }

    @Blocking
    public @NotNull List<HeadInfo> getRandom(int pageSize) {
        return getHeads(null, null, pageSize, 0);
    }

    // Returns pageSize + 1 if there is another page.
    @Blocking
    public @NotNull List<HeadInfo> getInCategory(@NotNull String category, int page, int pageSize) {
        return getHeads(null, category, pageSize + 1, page * pageSize);
    }

    @Blocking
    public @NotNull List<HeadInfo> getSuggestions(@NotNull String query, int count) {
        return getHeads(query, null, count, 0);
    }

    @Blocking
    public @NotNull List<HeadInfo> getSuggestions(@NotNull String query, int page, int pageSize) {
        return getHeads(query, null, pageSize + 1, page * pageSize);
    }

    private @NotNull List<HeadInfo> getHeads(@Nullable String query, @Nullable String category, int limit, int skip) {
        var url = new StringBuilder().append(BASE_URL)
                .append("?limit=").append(limit)
                .append("&skip=").append(skip);
        if (query != null)
            url.append("&query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8).replaceAll("\\+", "%20"));
        if (category != null)
            url.append("&category=").append(URLEncoder.encode(category, StandardCharsets.UTF_8).replaceAll("\\+", "%20"));

        var resp = doRequest("getHeads", HttpRequest.newBuilder()
                        .GET().uri(URI.create(url.toString()))
                        .header("Authorization", authToken),
                HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("Failed to fetch heads: " + resp.statusCode());
        return List.of(GSON.fromJson(resp.body(), HeadInfo[].class));
    }

}
