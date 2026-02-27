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
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.registry.StaticProtocolObject;
import net.minestom.server.sound.BuiltinSoundEvent;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public final class Autocompletors {

    private static final AutocompleteEngine<IndexableProtocolObject<Material>> materials = createEngine();
    private static final AutocompleteEngine<IndexableProtocolObject<Block>> fullBlocks = createEngine();
    private static final AutocompleteEngine<IndexableProtocolObject<BuiltinSoundEvent>> sounds = createEngine();

    static {
        for (var material : Material.values()) {
            materials.add(new IndexableProtocolObject<>(material));
        }
        for (var block : ItemUtils.PLACEABLE_ON_BLOCKS) {
            fullBlocks.add(new IndexableProtocolObject<>(block));
        }
        for (var sound : SoundEvent.values()) {
            if (!(sound instanceof BuiltinSoundEvent event)) continue;
            sounds.add(new IndexableProtocolObject<>(event));
        }
    }

    public static @NotNull List<Material> searchMaterials(@NotNull String query, int limit, Predicate<Material> predicate) {
        return search(materials, query, limit, predicate);
    }

    public static @NotNull List<Block> searchBlocks(@NotNull String query, int limit) {
        return search(fullBlocks, query, limit);
    }

    public static @NotNull List<Block> searchBlocks(@NotNull String query, int limit, Predicate<Block> predicate) {
        return search(fullBlocks, query, limit, predicate);
    }

    public static @NotNull List<BuiltinSoundEvent> searchSounds(@NotNull String query, int limit) {
        return search(sounds, query, limit, _ -> true);
    }

    public static <T extends Indexable> AutocompleteEngine<T> createEngine() {
        return new AutocompleteEngine.Builder<T>()
                .setIndex(new Indexer<>())
                .setAnalyzers(new LowerCaseTransformer(), new WordTokenizer())
                .build();
    }

    private static <T extends StaticProtocolObject<@NotNull T>> List<T> search(AutocompleteEngine<IndexableProtocolObject<T>> engine, String query, int limit) {
        return search(engine, query, limit, _ -> true);
    }

    private static <T extends StaticProtocolObject<@NotNull T>> List<T> search(AutocompleteEngine<IndexableProtocolObject<T>> engine, String query, int limit, Predicate<T> predicate) {
        List<T> output = new ArrayList<>(limit);
        for (var result : engine.search(query)) {
            if (output.size() >= limit) break;
            if (predicate.test(result.object())) {
                output.add(result.object());
            }
        }
        return output;
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

    private record IndexableProtocolObject<T extends StaticProtocolObject<@NotNull T>>(@NotNull T object) implements Indexable {
        @Override
        public List<String> getFields() {
            return List.of(object.name(), object.key().value(), object.key().value().replace("_", " "));
        }
    }
}
