package net.hollowcube.mapmaker.util;

import com.miguelfonseca.completely.AutocompleteEngine;
import com.miguelfonseca.completely.IndexAdapter;
import com.miguelfonseca.completely.data.Indexable;
import com.miguelfonseca.completely.data.ScoredObject;
import com.miguelfonseca.completely.text.analyze.tokenize.WordTokenizer;
import com.miguelfonseca.completely.text.analyze.transform.LowerCaseTransformer;
import com.miguelfonseca.completely.text.index.FuzzyIndex;
import com.miguelfonseca.completely.text.index.PatriciaTrie;
import com.miguelfonseca.completely.text.match.EditDistanceAutomaton;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public final class Autocompletors {

    private static final AutocompleteEngine<IndexableMaterial> materials = createEngine();

    static {
        for (var material : Material.values()) {
            materials.add(new IndexableMaterial(material));
        }
    }

    public static @NotNull List<Material> searchMaterials(@NotNull String query, int limit, Predicate<Material> predicate) {
        List<Material> output = new ArrayList<>(limit);
        for (IndexableMaterial material : materials.search(query)) {
            if (output.size() >= limit) break;
            if (predicate.test(material.material())) {
                output.add(material.material());
            }
        }
        return output;
    }

    public static <T extends Indexable> AutocompleteEngine<T> createEngine() {
        return new AutocompleteEngine.Builder<T>()
                .setIndex(new Indexer<>())
                .setAnalyzers(new LowerCaseTransformer(), new WordTokenizer())
                .build();
    }

    private static class Indexer<T> implements IndexAdapter<T> {
        private final FuzzyIndex<T> index = new PatriciaTrie<>();

        @Override
        public Collection<ScoredObject<T>> get(String token) {
            double threshold = Math.log(Math.max(token.length() - 1, 1));
            return index.getAny(new EditDistanceAutomaton(token, threshold));
        }

        @Override
        public boolean put(String token, @Nullable T value) {
            return index.put(token, value);
        }

        @Override
        public boolean remove(T value) {
            return index.remove(value);
        }
    }

    private record IndexableMaterial(@NotNull Material material) implements Indexable {
        @Override
        public List<String> getFields() {
            return List.of(material.name(), material.key().value(), material.key().value().replace("_", " "));
        }
    }

}
