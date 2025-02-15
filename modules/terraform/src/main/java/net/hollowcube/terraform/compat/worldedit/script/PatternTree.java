package net.hollowcube.terraform.compat.worldedit.script;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.terraform.TerraformRegistry;
import net.hollowcube.terraform.mask.script.Tree;
import net.hollowcube.terraform.pattern.*;
import net.hollowcube.terraform.util.script.ParseContext;
import net.hollowcube.terraform.util.script.ParseException;
import net.hollowcube.terraform.util.script.ParseTree;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.stream.Collectors;

public interface PatternTree extends ParseTree<Pattern> {

    record BlockState(
            @NotNull NamespaceId namespaceId,
            @NotNull PropertyList props
    ) implements PatternTree {
        static final List<String> BLOCK_NAMESPACES = Block.values().stream()
                .map(block -> block.namespace().namespace() + ":")
                .collect(Collectors.toSet()).stream().toList();
        static final List<NamespaceID> BLOCKS = Block.values().stream()
                .map(Block::namespace).sorted().toList();

        @Override
        public int start() {
            return namespaceId().start();
        }

        @Override
        public int end() {
            return props().end();
        }

        @Override
        public @NotNull Pattern into(@NotNull ParseContext context) throws ParseException {
            var block = namespaceId.toBlock();
            var propertyList = props().toPropertyList();

            // Apply the properties, handling the case where there is an invalid property for the particular block
            if (!propertyList.isEmpty()) {
                var possibilities = PropertyList.POSSIBLE_PROPERTIES.getOrDefault(block.id(), Object2ObjectMaps.emptyMap());
                for (var entry : propertyList.entrySet()) {
                    var propertyKey = entry.getKey();
                    var possibleValues = possibilities.get(propertyKey);
                    if (possibleValues == null)
                        throw new ParseException(props().start(), props().end(), "no such property: " + propertyKey);
                    if (!possibleValues.contains(entry.getValue()))
                        throw new ParseException(props().start(), props().end(), "no such value for property " + propertyKey + ": " + entry.getValue());
                }
                block = block.withProperties(propertyList);
            }

            // Remap the block to the Terraform registry block
            var registryBlock = context.registry().blockState(block.stateId());
            return new BlockPattern(registryBlock);
        }

        @Override
        public void suggest(@NotNull TerraformRegistry registry, @NotNull Suggestion suggestion) {
            if (props.openBracket == -1) {
                //todo probably blocks and block namespaces should come from registry? ie to handle custom blocks.
                // Though probably these should not be available. In fact worldediting checkpoint plates maybe should
                // delete them?
                // However we should support peoples resource packs in the future and allow them to upload a list of custom
                // block states which are added here automatically
                namespaceId.suggest(suggestion, BLOCKS, BLOCK_NAMESPACES, true);
            } else {
                try {
                    props.suggest(suggestion, namespaceId.toBlock());
                } catch (ParseException e) {
                    // The block is invalid, so just return suggestions for any property.
                    props.suggest(suggestion, null);
                }
            }
        }
    }

    record LegacyBlock(
            int start, int end,
            int colon, // -1 if not present
            int id,
            int data // -1 if not present
    ) implements PatternTree {
        @Override
        public @NotNull Pattern into(@NotNull ParseContext context) throws ParseException {
            var data = this.data == -1 ? 0 : this.data;
            var legacyBlock = context.registry().legacyBlockState(id, data);
            if (legacyBlock == null)
                throw new ParseException(start, end, String.format("no such block: %d:%d", id, data));
            return new BlockPattern(legacyBlock);
        }

        @Override
        public void suggest(@NotNull TerraformRegistry registry, @NotNull Suggestion suggestion) {
            // Intentionally do nothing
        }
    }

    record RandomState(
            int start, int end,
            @Nullable NamespaceId namespaceId
    ) implements PatternTree {
        @SuppressWarnings({"DataFlowIssue"})
        private static final List<String> DEFAULT_SUGGESTIONS = BlockState.BLOCKS.stream()
                .filter(id -> Block.fromNamespaceId(id).possibleStates().size() > 1)
                .map(NamespaceID::path).limit(20).toList();

        @Override
        public @NotNull Pattern into(@NotNull ParseContext context) throws ParseException {
            if (namespaceId == null)
                throw new ParseException(end, end, "expected block");
            return new RandomStatePattern(namespaceId.toBlock().id());
        }

        @Override
        public void suggest(@NotNull TerraformRegistry registry, @NotNull Suggestion suggestion) {
            suggestion.setAbsolute(start + 1);
            if (namespaceId == null) {
                // The default suggestions are the first 20 blocks which have properties.
                DEFAULT_SUGGESTIONS.forEach(suggestion::add);
            } else {
                // Note that we do suggest any block here, even ones without properties. It is valid to use them,
                // just does nothing.
                namespaceId.suggest(suggestion, BlockState.BLOCKS, BlockState.BLOCK_NAMESPACES, false);
            }
        }
    }

    record Tag(
            int start, int end,
            int star, // -1 if not present
            @Nullable NamespaceId namespaceId
    ) implements PatternTree {
        //todo this should come from the registry realistically
        private static final Set<String> BLOCK_TAGS_NAMESPACES = MinecraftServer.getTagManager().getTagMap()
                .get(net.minestom.server.gamedata.tags.Tag.BasicType.BLOCKS).stream()
                .map(tag -> tag.getName().namespace()).collect(Collectors.toSet());
        private static final List<NamespaceID> BLOCK_TAGS = MinecraftServer.getTagManager().getTagMap()
                .get(net.minestom.server.gamedata.tags.Tag.BasicType.BLOCKS).stream()
                .map(net.minestom.server.gamedata.tags.Tag::getName).toList();

        @Override
        public @NotNull Pattern into(@NotNull ParseContext context) throws ParseException {
            if (namespaceId == null)
                throw new ParseException(end, end, "expected tag");
            var tagId = namespaceId.toNamespaceId();
            if (!BLOCK_TAGS.contains(tagId))
                throw new ParseException(namespaceId.start(), namespaceId.end(), "no such tag: " + tagId);
            return new TagPattern(tagId.asString(), star != -1);
        }

        @Override
        public void suggest(@NotNull TerraformRegistry registry, @NotNull Suggestion suggestion) {
            suggestion.setAbsolute(start + 2);

            // If there is no star and we have no nsid input, suggest a star
            if (star == -1 && namespaceId == null) {
                suggestion.add("*", Component.text("Choose a random state for each block"));
            } else if (star != -1) {
                suggestion.setAbsolute(star + 1);
            }

            if (namespaceId == null) {
                for (int i = 0; i < Math.min(20, BLOCK_TAGS.size()); i++) {
                    suggestion.add(BLOCK_TAGS.get(i).path());
                }
            } else {
                namespaceId.suggest(suggestion, BLOCK_TAGS, BLOCK_TAGS_NAMESPACES, false);
            }
        }
    }

    record TypeStateApply(
            int start, int end,
            @Nullable NamespaceId namespaceId,
            @Nullable PropertyList props
    ) implements PatternTree {

        @Override
        public @NotNull Pattern into(@NotNull ParseContext context) throws ParseException {
            if (namespaceId == null && props == null)
                throw new ParseException(end, end, "expected block or properties");
            if (props != null && props.size() == 0)
                throw new ParseException(start(), end(), "block id or properties are required");

            int blockId = namespaceId == null ? -1 : namespaceId.toBlock().id();
            Map<String, String> props = this.props == null ? Map.of() : this.props.toPropertyList();

            return new TypeStatePattern(blockId, props);
        }

        @Override
        public void suggest(@NotNull TerraformRegistry registry, @NotNull Suggestion suggestion) {
            suggestion.setAbsolute(start + 1);
            if (namespaceId == null && props == null) {
                // Suggest both an open bracket and some random blocks to start
                suggestion.add("[", Component.text("Replace by block properties, not block id"));
                for (int i = 0; i < Math.min(20, BlockState.BLOCKS.size()); i++) {
                    suggestion.add(BlockState.BLOCKS.get(i).path());
                }
                return;
            }

            // We make suggestions on the namespace only if the properties are missing. If the properties are present
            // it always means that we are done with the namespace.
            if (props == null) {
                // Even though it is valid to have a block and properties we do not suggest adding a [ after the namespace
                // because the use case for block id AND properties is limited.
                namespaceId.suggest(suggestion, BlockState.BLOCKS, BlockState.BLOCK_NAMESPACES, false);
            } else if (namespaceId != null) {
                try {
                    props.suggest(suggestion, namespaceId.toBlock());
                } catch (ParseException e) {
                    // The block is invalid, so just return suggestions for any property.
                    props.suggest(suggestion, null);
                }
            } else {
                // Suggest any property
                props.suggest(suggestion, null);
            }
        }
    }

    record WeightedList(
            int trailingComma, // -1 if not present
            @NotNull List<PatternTree> entries
    ) implements PatternTree {

        public WeightedList {
            Check.argCondition(entries.isEmpty(), "Weighted list must have at least one entry");
            entries = List.copyOf(entries);
        }

        @Override
        public int start() {
            return entries.getFirst().start();
        }

        @Override
        public int end() {
            return entries.getLast().end();
        }

        @Override
        public @NotNull Pattern into(@NotNull ParseContext context) throws ParseException {
            if (trailingComma != -1)
                throw new ParseException(trailingComma, trailingComma, "expected pattern");

            var results = new ArrayList<RandomPatternPattern.Entry>();
            var total = 0;

            for (var entry : this.entries) {
                if (entry instanceof Weighted(int start, int end, int weight, PatternTree pattern)) {
                    ParseException.requireNonNull(pattern, start, end,"expected pattern");
                    var child = pattern.into(context);
                    results.add(new RandomPatternPattern.Entry(weight, child));
                    total += weight;
                } else {
                    results.add(new RandomPatternPattern.Entry(1, entry.into(context)));
                    total += 1;
                }
            }

            return new RandomPatternPattern(results, total);
        }

        @Override
        public void suggest(@NotNull TerraformRegistry registry, @NotNull Suggestion suggestion) {
            // If there is a trailing comma then suggest a new entry (empty results), otherwise defer to the final entry.
            if (trailingComma != -1) {
                fillEmptySuggestion(registry, suggestion, trailingComma + 1);
            } else {
                // shift to the end and suggest
                var lastEntry = entries.getLast();
                suggestion.setAbsolute(lastEntry.start());
                lastEntry.suggest(registry, suggestion);
            }
        }
    }

    /**
     * Weighted is an intermediate pattern collapsed by WeightedList, it should never be returned.
     */
    record Weighted(
            int start, int end,
            int weight,
            @Nullable PatternTree pattern
    ) implements PatternTree {
        @Override
        public @NotNull Pattern into(@NotNull ParseContext context) throws ParseException {
            throw new ParseException(start, end, "intermediate, this should not happen.");
        }

        @Override
        public void suggest(@NotNull TerraformRegistry registry, @NotNull Suggestion suggestion) {
            // If pattern is missing it means the user typed something like `5%` and we should suggest the pattern to
            // follow, so show the empty suggestions. Otherwise we should always defer to the suggestion.
            if (pattern == null) {
                fillEmptySuggestion(registry, suggestion, end);
            } else {
                pattern.suggest(registry, suggestion);
            }
        }
    }

    record Hand(int start, int end, @NotNull PlayerHand hand) implements PatternTree {

        @Override
        public @NotNull Pattern into(@NotNull ParseContext context) throws ParseException {
            var item = context.getPlayer().getItemInHand(this.hand).material();
            var block = ParseException.requireNonNull(
                    item == Material.AIR ? Block.AIR : item.block(),
                    start, end, "no block in hand"
            );
            return new BlockPattern(context.registry().blockState(block.stateId()));
        }
    }

    record Error(int start, int end) implements PatternTree {
        @Override
        public @NotNull Pattern into(@NotNull ParseContext context) throws ParseException {
            throw new ParseException(start, end, "not implemented");
        }
    }

    // Helpers

    record NamespaceId(
            int start, int end,
            int colon, // -1 if not present
            @NotNull String left,
            @Nullable String right
    ) {

        public @NotNull NamespaceID toNamespaceId() throws ParseException {
            if (colon != -1 && right == null)
                throw new ParseException(end, end, "expected block name");
            var namespace = right == null ? "minecraft" : left;
            var path = right == null ? left : right;
            return NamespaceID.from(namespace, path);
        }

        public @NotNull Block toBlock() throws ParseException {
            var block = Block.fromNamespaceId(toNamespaceId());
            if (block == null)
                throw new ParseException(start(), end, "no such block: " + toNamespaceId());
            return block;
        }

        public void suggest(
                @NotNull Suggestion suggestion,
                @NotNull Collection<NamespaceID> values,
                @NotNull Collection<String> namespaces,
                boolean hasProperties
        ) {
            suggestion.setAbsolute(start);
            var hasNamespace = this.colon != -1;
            var path = !hasNamespace ? this.left
                    : (this.right == null ? "" : this.right);

            for (var block : values) {
                // If we hit an exact match then we should suggest a [ to start the property list.
                if (block.path().equals(path)) {
                    suggestion.clear();
                    if (hasProperties && hasAnyProps(block.asString())) {
                        suggestion.setStart(suggestion.getStart() + end);
                        suggestion.add("[");
                    }
                    return;
                }

                if (block.path().startsWith(path)) {
                    suggestion.add(hasNamespace ? block.asString() : block.path());
                }

                if (suggestion.getEntries().size() >= 20)
                    return;
            }

            if (colon == -1) {
                for (var namespace : namespaces) {
                    if (namespace.startsWith(this.left)) {
                        suggestion.add(namespace);
                    }

                    if (suggestion.getEntries().size() >= 20)
                        return;
                }
            }
        }

        private static boolean hasAnyProps(@NotNull String namespaceId) {
            var block = Block.fromNamespaceId(namespaceId);
            assert block != null;
            return PropertyList.POSSIBLE_PROPERTIES.containsKey(block.id());
        }
    }

    //todo this only works for block properties currently.
    record PropertyList(
            int start, int end,
            // Both -1 if not present
            int openBracket, int closeBracket,
            int trailingComma,
            @NotNull List<Property> properties
    ) {
        // block id -> property name -> possible values. Missing means no properties.
        private static final Int2ObjectMap<Object2ObjectMap<String, List<String>>> POSSIBLE_PROPERTIES = new Int2ObjectArrayMap<>();
        // property name -> possible values across all blocks
        private static final Object2ObjectMap<String, List<String>> PROPERTIES_BY_NAME = new Object2ObjectAVLTreeMap<>(Comparator.naturalOrder());

        public @UnknownNullability Property get(int index) {
            return properties.get(index);
        }

        public int size() {
            return properties.size();
        }

        public @NotNull Map<String, String> toPropertyList() throws ParseException {
            if (openBracket == -1 && closeBracket == -1) return Map.of();
            if (closeBracket == -1)
                throw new ParseException(end, end, "expected property or ']'");
            if (trailingComma != -1)
                throw new ParseException(trailingComma, trailingComma, "expected property");

            var result = new HashMap<String, String>();
            for (var entry : properties) {
                if (entry.equals == -1)
                    throw new ParseException(entry.end, entry.end, "expected '='");
                if (entry.value == null)
                    throw new ParseException(entry.end, entry.end, "expected value");
                result.put(entry.key, entry.value);
            }
            return result;
        }

        public void suggest(@NotNull Suggestion suggestion, @Nullable Block block) {
            if (closeBracket != -1) return;

            if (block == null) {
                suggestFromPropList(suggestion, PROPERTIES_BY_NAME);
                return;
            }

            var possible = POSSIBLE_PROPERTIES.get(block.id());
            if (possible == null) {
                // The block has no properties so simply suggest closing the bracket.
                suggestion.setStart(suggestion.getStart() + end);
                suggestion.add("]");
                return;
            }

            suggestFromPropList(suggestion, possible);
        }

        private void suggestFromPropList(@NotNull Suggestion suggestion, @NotNull Map<String, List<String>> validProperties) {
            if (closeBracket != -1) return;

            // If we are at the start of a property, handle that.
            if (size() == 0 || trailingComma != -1) {
                suggestion.setAbsolute(trailingComma == -1 ? openBracket + 1 : trailingComma);
                suggest:
                for (var entry : validProperties.entrySet()) {

                    // Do not suggest a previously used entry
                    for (var prop : properties) {
                        if (prop.key.equals(entry.getKey())) {
                            continue suggest;
                        }
                    }

                    suggestion.add(entry.getKey());
                }
                return;
            }

            // Otherwise, delegate to the last property
            properties.getLast().suggest(suggestion, validProperties);
        }

        public record Property(
                int start, int end,
                int equals, // -1 if not present
                @NotNull String key,
                @Nullable String value
        ) implements Tree {
            private void suggest(@NotNull Suggestion suggestion, @NotNull Map<String, List<String>> validProperties) {

                // Start by counting how many property names match the current key
                int matching = 0;
                boolean exact = false;
                List<String> values = null; // Last matched. Only relevant when matching = 1
                for (var entry : validProperties.entrySet()) {
                    exact |= entry.getKey().equals(key);
                    if (entry.getKey().startsWith(key)) {
                        matching++;
                        values = entry.getValue();
                    }
                }

                // If there are no matches, suggest nothing.
                if (matching == 0) return;

                if (equals == -1) {
                    // If there is exactly one match, suggest the equals
                    if (matching == 1 && exact) {
                        suggestion.setAbsolute(end);
                        suggestion.add("=");
                    } else {
                        suggestion.setAbsolute(start);
                        for (var key : validProperties.keySet()) {
                            if (key.startsWith(this.key)) {
                                suggestion.add(key);
                            }
                        }
                    }
                } else {
                    // If we have an equals then there must be exactly one match
                    if (matching != 1) return;
                    suggestion.setAbsolute(equals + 1);

                    var value = this.value == null ? "" : this.value;
                    for (var property : validProperties.get(key)) {
                        // If we have an exact match then suggest closing it
                        if (property.equals(value)) {
                            suggestion.clear();
                            suggestion.setAbsolute(end);
                            suggestion.add("]");
                            return;
                        }

                        // Add if similar in this case
                        if (property.startsWith(value)) {
                            suggestion.add(property);
                        }
                    }
                }
            }
        }

        static {
            var possibleProperties = new Int2ObjectOpenHashMap<Map<String, Set<String>>>();
            var propertiesByName = new HashMap<String, Set<String>>();

            for (int id = 0; id < 10_000; id++) {
                var block = Block.fromBlockId(id);
                if (block == null) break;

                var localProps = new HashMap<String, Set<String>>();
                for (var state : block.possibleStates()) {
                    for (var entry : state.properties().entrySet()) {
                        var key = entry.getKey();
                        var value = entry.getValue();
                        localProps.computeIfAbsent(key, k -> new HashSet<>()).add(value);
                        propertiesByName.computeIfAbsent(key, k -> new HashSet<>()).add(value);
                    }
                }

                if (!localProps.isEmpty()) possibleProperties.put(id, localProps);
            }

            for (var entry : possibleProperties.int2ObjectEntrySet()) {
                var map = new Object2ObjectAVLTreeMap<String, List<String>>(Comparator.naturalOrder());
                for (var prop : entry.getValue().entrySet()) {
                    var sorted = new ArrayList<>(prop.getValue());
                    sorted.sort(Comparator.naturalOrder());
                    map.put(prop.getKey(), sorted);
                }
                POSSIBLE_PROPERTIES.put(entry.getIntKey(), map);
            }

            for (var entry : propertiesByName.entrySet()) {
                var sorted = new ArrayList<>(entry.getValue());
                sorted.sort(Comparator.naturalOrder());
                PROPERTIES_BY_NAME.put(entry.getKey(), sorted);
            }
        }
    }

    static void fillEmptySuggestion(@NotNull TerraformRegistry registry, @NotNull Suggestion suggestion, int pos) {
        suggestion.setAbsolute(pos);

        //todo not so sure about this, may just suggest blocks here always. Worldedit suggests a mess also.
        suggestion.add("*", Component.text("Choose a random state for the given block"));
        suggestion.add("^", Component.text("Replace blocks or properties leaving the rest unmodified"));

        suggestion.add("##", Component.text("Get blocks from a block tag"));
        suggestion.add("##stairs");
        suggestion.add("##slabs");

//        suggestion.add("#", Component.text("Call a named pattern function")); //todo this should be a sample of some functions

        suggestion.add("air");
        suggestion.add("stone");
    }

}
