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

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class Autocompletors {

    public static final Set<Material> MATERIAL_BLACKLIST = Set.of(
            Material.AIR, // Obvious
            Material.BUNDLE, // Adds a slot lore element
            // Stuff that glows
            Material.DEBUG_STICK, Material.NETHER_STAR, Material.EXPERIENCE_BOTTLE, Material.LIGHT,
            Material.ENCHANTED_GOLDEN_APPLE, Material.ENCHANTED_BOOK, Material.END_CRYSTAL,
            // Stuff that moves
            Material.SCULK_SENSOR, Material.CALIBRATED_SCULK_SENSOR, Material.RECOVERY_COMPASS
    );

    private static final AutocompleteEngine<IndexableMaterial> materials = new AutocompleteEngine.Builder<IndexableMaterial>()
            .setIndex(createDefaultIndexAdapter())
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
            if (MATERIAL_BLACKLIST.contains(material)) continue;
            materials.add(new IndexableMaterial(material));
        }
    }

    public static @NotNull List<Material> mapIconMaterial(@NotNull String input, int limit) {
        return materials.search(input, limit).stream().map(IndexableMaterial::material).toList();
    }

    public static <T> @NotNull IndexAdapter<T> createDefaultIndexAdapter() {
        return new IndexAdapter<>() {
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
        };
    }

}
