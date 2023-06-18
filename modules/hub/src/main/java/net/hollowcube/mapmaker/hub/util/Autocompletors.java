package net.hollowcube.mapmaker.hub.util;

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

import java.util.Collection;
import java.util.List;

public final class Autocompletors {

    private static final AutocompleteEngine<IndexableMaterial> materials = new AutocompleteEngine.Builder<IndexableMaterial>()
            .setIndex(new IndexAdapter<>() {
                private final FuzzyIndex<IndexableMaterial> index = new PatriciaTrie<>();

                @Override
                public Collection<ScoredObject<IndexableMaterial>> get(String token) {
                    double threshold = Math.log(Math.max(token.length() - 1, 1));
                    return index.getAny(new EditDistanceAutomaton(token, threshold));
                }

                @Override
                public boolean put(String token, @Nullable IndexableMaterial value) {
                    return index.put(token, value);
                }

                @Override
                public boolean remove(IndexableMaterial value) {
                    return index.remove(value);
                }
            })
            .setAnalyzers(new LowerCaseTransformer(), new WordTokenizer())
            .build();

    private record IndexableMaterial(
            @NotNull Material material
    ) implements Indexable {
        @Override
        public List<String> getFields() {
            return List.of(material.name(), material.namespace().path(), material.namespace().path().replace("_", " "));
        }
    }

    static {
        for (var material : Material.values()) {
            if (material.id() == Material.AIR.id()) continue;
            materials.add(new IndexableMaterial(material));
        }
    }

    public static @NotNull List<Material> material(@NotNull String input, int limit) {
        return materials.search(input, limit).stream().map(IndexableMaterial::material).toList();
    }

}
