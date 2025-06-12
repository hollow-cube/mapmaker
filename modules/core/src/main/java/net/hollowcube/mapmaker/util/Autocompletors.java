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
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class Autocompletors {

    private static final AutocompleteEngine<IndexableMaterial> materials = createEngine();
    private static final AutocompleteEngine<IndexableBlock> fullBlocks = createEngine();

    private static final Set<Block> EXCLUDED_BLOCKS = Set.of(
            Block.STRUCTURE_BLOCK, Block.JIGSAW, Block.BARRIER,
            Block.COMMAND_BLOCK, Block.CHAIN_COMMAND_BLOCK, Block.REPEATING_COMMAND_BLOCK,
            Block.TEST_BLOCK, Block.TEST_INSTANCE_BLOCK,

            // Blocks with bad prediction
            Block.BEACON, Block.SMITHING_TABLE, Block.CRAFTER, Block.CRAFTING_TABLE, Block.FURNACE, Block.BLAST_FURNACE,
            Block.SMOKER, Block.CARTOGRAPHY_TABLE, Block.LOOM, Block.NOTE_BLOCK, Block.BARREL,
            Block.SHULKER_BOX, Block.BLACK_SHULKER_BOX, Block.BLUE_SHULKER_BOX, Block.BROWN_SHULKER_BOX, Block.CYAN_SHULKER_BOX,
            Block.GRAY_SHULKER_BOX, Block.GREEN_SHULKER_BOX, Block.LIGHT_BLUE_SHULKER_BOX, Block.LIGHT_GRAY_SHULKER_BOX,
            Block.LIME_SHULKER_BOX, Block.MAGENTA_SHULKER_BOX, Block.ORANGE_SHULKER_BOX, Block.PINK_SHULKER_BOX,
            Block.PURPLE_SHULKER_BOX, Block.RED_SHULKER_BOX, Block.WHITE_SHULKER_BOX, Block.YELLOW_SHULKER_BOX,
            Block.DISPENSER, Block.DROPPER
    );

    static {
        for (var material : Material.values()) {
            materials.add(new IndexableMaterial(material));
        }
        blocksLoop:
        for (var block : Block.values()) {
            var material = block.registry().material();
            if (material == null) continue; // Non block item
            if (!material.key().equals(block.key())) continue; // Weird block item (like flint and steel)
            if (EXCLUDED_BLOCKS.contains(block))
                continue; // Blocks with bad prediction

            // Only add blocks that are full cubes
            var shape = block.registry().collisionShape();
            for (var face : BlockFace.values()) {
                if (!shape.isFaceFull(face))
                    continue blocksLoop;
            }

            fullBlocks.add(new IndexableBlock(block));
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

    public static @NotNull List<Block> searchBlocks(@NotNull String query, int limit) {
        return searchBlocks(query, limit, _ -> true);
    }

    public static @NotNull List<Block> searchBlocks(@NotNull String query, int limit, Predicate<Block> predicate) {
        List<Block> output = new ArrayList<>(limit);
        for (var block : fullBlocks.search(query)) {
            if (output.size() >= limit) break;
            if (predicate.test(block.block())) {
                output.add(block.block());
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

    private record IndexableBlock(@NotNull Block block) implements Indexable {
        @Override
        public List<String> getFields() {
            return List.of(block.name(), block.key().value(), block.key().value().replace("_", " "));
        }
    }

}
