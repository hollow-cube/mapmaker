package net.hollowcube.mapmaker.map.hdb;

import com.miguelfonseca.completely.AutocompleteEngine;
import com.miguelfonseca.completely.text.analyze.tokenize.WordTokenizer;
import com.miguelfonseca.completely.text.analyze.transform.LowerCaseTransformer;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeadDatabase {
    private static final Logger logger = LoggerFactory.getLogger(HeadDatabase.class);

    private final String MINECRAFT_HEADS_LIST = "https://minecraft-heads.4lima.de/csv/2022-02-25-ZgFDreHnLiGvHdf3RFfgg/Custom-Head-DB.csv";

    private Map<String, HeadInfo> heads;
    private Map<String, List<HeadInfo>> categories;

    private HeadInfo[] headsArray = new HeadInfo[0];
    private final AutocompleteEngine<HeadInfo> autocompletor = new AutocompleteEngine.Builder<HeadInfo>()
            .setIndex(Autocompletors.createDefaultIndexAdapter())
            .setAnalyzers(new LowerCaseTransformer(), new WordTokenizer())
            .build();

    private final Map<String, ItemStack> categoryIcons = Map.ofEntries(
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

    public HeadDatabase() {
        FutureUtil.submitVirtual(this::loadHeadList);
    }

    public @NotNull List<HeadInfo> heads(@NotNull String category) {
        return categories.getOrDefault(category, List.of());
    }

    public boolean isLoaded() {
        return this.heads != null;
    }

    public @NotNull Collection<String> categories() {
        return categories.keySet();
    }

    public @NotNull ItemStack categoryIcon(@NotNull String category) {
        return categoryIcons.getOrDefault(category, ItemStack.of(Material.BARRIER));
    }

    public int size() {
        return heads.size();
    }

    public int size(@NotNull String category) {
        return heads(category).size();
    }

    public @NotNull Stream<HeadInfo> random() {
        return ThreadLocalRandom.current()
                .ints(0, heads.size())
                .mapToObj(i -> headsArray[i]);
    }

    public List<HeadInfo> suggest(@NotNull String input, int limit) {
        return autocompletor.search(input, limit);
    }

    @Blocking
    private void loadHeadList() {
        try {
            var req = HttpRequest.newBuilder()
                    .GET().uri(URI.create(MINECRAFT_HEADS_LIST))
                    .header("User-Agent", AbstractHttpService.userAgent)
                    .build();
            var res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofLines());
            if (res.statusCode() != 200) throw new RuntimeException("non-200 response: " + res.statusCode());

            var heads = new HashMap<String, HeadInfo>();
            var categories = new LinkedHashMap<String, List<HeadInfo>>();
            res.body().map(HeadInfo::fromLine)
                    .filter(Objects::nonNull)
                    .forEach(info -> {
                        heads.put(info.id(), info);
                        categories.computeIfAbsent(info.category(), k -> new ArrayList<>()).add(info);
                    });
            this.heads = Map.copyOf(heads);
            this.categories = Collections.unmodifiableMap(categories);
        } catch (Exception e) {
            logger.error("Failed to load head list", e);
            this.heads = Map.of();
            this.categories = Map.of();
        }

        headsArray = heads.values().toArray(new HeadInfo[0]);
        autocompletor.addAll(heads.values());
        logger.info("Loaded {} heads in {} categories", heads.size(), categories.size());
    }

}
