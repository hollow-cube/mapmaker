package net.hollowcube.mapmaker.map.hdb;

import com.miguelfonseca.completely.AutocompleteEngine;
import com.miguelfonseca.completely.text.analyze.tokenize.WordTokenizer;
import com.miguelfonseca.completely.text.analyze.transform.LowerCaseTransformer;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.Autocompletors;
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

    public HeadDatabase() {
        FutureUtil.submitVirtual(this::loadHeadList);
    }

    public boolean isLoaded() {
        return this.heads != null;
    }

    public @NotNull Collection<String> categories() {
        return categories.keySet();
    }

    public int size() {
        return heads.size();
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
            var categories = new HashMap<String, List<HeadInfo>>();
            res.body().map(HeadInfo::fromLine)
                    .filter(Objects::nonNull)
                    .forEach(info -> {
                        heads.put(info.id(), info);
                        categories.computeIfAbsent(info.category(), k -> new ArrayList<>()).add(info);
                    });
            this.heads = Map.copyOf(heads);
            this.categories = Map.copyOf(categories);
        } catch (Exception e) {
            logger.error("Failed to load head list", e);
            this.heads = Map.of();
            this.categories = Map.of();
        }

        logger.info("Loaded {} heads in {} categories", heads.size(), categories.size());
        headsArray = heads.values().toArray(new HeadInfo[0]);
        logger.info("Loaded {} heads in {} categories PART 2", heads.size(), categories.size());
        autocompletor.addAll(heads.values());
        logger.info("Loaded {} heads in {} categories PART 3", heads.size(), categories.size());
    }

}
