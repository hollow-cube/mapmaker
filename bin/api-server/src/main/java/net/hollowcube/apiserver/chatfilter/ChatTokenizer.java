package net.hollowcube.apiserver.chatfilter;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Ported from huggingface tokenizers 0.23.3:
/// - [added tokens](https://github.com/huggingface/tokenizers/blob/v0.23.3/tokenizers/src/tokenizer/added_vocabulary.rs)
/// - [Metaspace](https://github.com/huggingface/tokenizers/blob/v0.23.3/tokenizers/src/pre_tokenizers/metaspace.rs)
/// - [BPE and byte fallback](https://github.com/huggingface/tokenizers/blob/v0.23.3/tokenizers/src/models/bpe/model.rs)
/// - [merge order](https://github.com/huggingface/tokenizers/blob/v0.23.3/tokenizers/src/models/bpe/word.rs)
public final class ChatTokenizer {
    private static final byte[] MAGIC = "HCTK".getBytes(StandardCharsets.US_ASCII);
    private static final int FORMAT = 1;
    private static final char SPACE = '▁';

    private record Added(String content, int id, boolean lstrip) {}

    private final int maxLength;
    private final int bos;
    private final int eos;
    private final int unk;
    private final Map<Integer, Integer> chars;
    private final int[] bytes;
    /// `left << 32 | right`, sorted, with [#mergeRanks] alongside; a merge's rank is its index
    /// in the file, and lower merges first.
    private final long[] mergeKeys;
    private final int[] mergeRanks;
    private final int[] mergeResults;
    private final Map<Character, List<Added>> addedByFirst;
    private final int[] remap;

    private ChatTokenizer(
        int maxLength,
        int bos,
        int eos,
        int unk,
        Map<Integer, Integer> chars,
        int[] bytes,
        long[] mergeKeys,
        int[] mergeRanks,
        int[] mergeResults,
        Map<Character, List<Added>> addedByFirst,
        int[] remap
    ) {
        this.maxLength = maxLength;
        this.bos = bos;
        this.eos = eos;
        this.unk = unk;
        this.chars = chars;
        this.bytes = bytes;
        this.mergeKeys = mergeKeys;
        this.mergeRanks = mergeRanks;
        this.mergeResults = mergeResults;
        this.addedByFirst = addedByFirst;
        this.remap = remap;
    }

    public static ChatTokenizer load(Path file) throws IOException {
        var buf = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);
        var magic = new byte[4];
        buf.get(magic);
        if (!Arrays.equals(magic, MAGIC) || buf.getInt() != FORMAT)
            throw new IOException(file + " is not a format " + FORMAT + " chat filter tokenizer");

        int maxLength = buf.getInt(), bos = buf.getInt(), eos = buf.getInt(), unk = buf.getInt();

        int charCount = buf.getInt();
        var chars = new HashMap<Integer, Integer>(charCount * 2);
        for (int i = 0; i < charCount; i++) chars.put(buf.getInt(), buf.getInt());

        var bytes = new int[256];
        for (int i = 0; i < 256; i++) bytes[i] = buf.getInt();

        int mergeCount = buf.getInt();
        var keys = new long[mergeCount];
        var results = new int[mergeCount];
        for (int i = 0; i < mergeCount; i++) {
            keys[i] = pair(buf.getInt(), buf.getInt());
            results[i] = buf.getInt();
        }
        var order = new Integer[mergeCount];
        for (int i = 0; i < mergeCount; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingLong(i -> keys[i]));
        var mergeKeys = new long[mergeCount];
        var mergeRanks = new int[mergeCount];
        for (int i = 0; i < mergeCount; i++) {
            mergeKeys[i] = keys[order[i]];
            mergeRanks[i] = order[i];
        }

        int addedCount = buf.getInt();
        var addedByFirst = new HashMap<Character, List<Added>>();
        for (int i = 0; i < addedCount; i++) {
            int id = buf.getInt();
            boolean lstrip = buf.get() != 0;
            var content = new byte[Short.toUnsignedInt(buf.getShort())];
            buf.get(content);
            var added = new Added(new String(content, StandardCharsets.UTF_8), id, lstrip);
            addedByFirst.computeIfAbsent(added.content.charAt(0), _ -> new ArrayList<>())
                .add(added);
        }
        for (var list : addedByFirst.values())
            list.sort(Comparator.comparingInt((Added a) -> a.content.length()).reversed());

        var remap = new int[buf.getInt()];
        for (int i = 0; i < remap.length; i++) remap[i] = buf.getInt();

        return new ChatTokenizer(
            maxLength,
            bos,
            eos,
            unk,
            chars,
            bytes,
            mergeKeys,
            mergeRanks,
            results,
            addedByFirst,
            remap
        );
    }

    /// The model's input ids for `text`: wrapped in bos/eos, cut to the length it was trained at,
    /// and remapped into its pruned vocabulary.
    public long[] encode(String text) {
        var ids = new IntList();
        int plainStart = 0;
        for (int i = 0; i < text.length();) {
            var added = addedAt(text, i);
            if (added == null) {
                i++;
                continue;
            }
            int start = i;
            if (added.lstrip) {
                while (start > plainStart) {
                    int cp = text.codePointBefore(start);
                    if (!isWhitespace(cp)) break;
                    start -= Character.charCount(cp);
                }
            }
            encodePlain(text.substring(plainStart, start), ids);
            ids.add(added.id);
            i += added.content.length();
            plainStart = i;
        }
        encodePlain(text.substring(plainStart), ids);

        int kept = Math.min(ids.size, maxLength - 2);
        var out = new long[kept + 2];
        out[0] = remap[bos];
        for (int i = 0; i < kept; i++) out[i + 1] = remap[ids.values[i]];
        out[kept + 1] = remap[eos];
        return out;
    }

    /// Leftmost-longest, like the aho-corasick automaton tokenizers splits added tokens with.
    private @Nullable Added addedAt(String text, int i) {
        var candidates = addedByFirst.get(text.charAt(i));
        if (candidates == null) return null;
        for (var added : candidates) {
            if (text.startsWith(added.content, i)) return added;
        }
        return null;
    }

    private void encodePlain(String segment, IntList ids) {
        if (segment.isEmpty()) return;
        var normalized = segment.replace(' ', SPACE);
        if (normalized.charAt(0) != SPACE) normalized = SPACE + normalized;

        // Split before every ▁, which is Metaspace's MergedWithNext.
        int start = 0;
        for (int i = 1; i <= normalized.length(); i++) {
            if (i == normalized.length() || normalized.charAt(i) == SPACE) {
                encodeWord(normalized, start, i, ids);
                start = i;
            }
        }
    }

    private void encodeWord(String text, int from, int to, IntList ids) {
        var symbols = new IntList();
        boolean lastUnknown = false;
        for (int i = from; i < to;) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);

            var id = chars.get(cp);
            if (id != null) {
                symbols.add(id);
                lastUnknown = false;
                continue;
            }
            var fallback = byteFallback(cp);
            if (fallback != null) {
                for (int b : fallback) symbols.add(b);
                lastUnknown = false;
                continue;
            }
            if (!lastUnknown) symbols.add(unk);
            lastUnknown = true;
        }

        // Always the lowest-ranked pair, leftmost on a tie, which is the order tokenizers' merge
        // queue pops them in. Words are short enough that rescanning beats keeping a heap.
        while (symbols.size > 1) {
            int best = -1, bestRank = Integer.MAX_VALUE;
            for (int i = 0; i + 1 < symbols.size; i++) {
                int rank = rank(symbols.values[i], symbols.values[i + 1]);
                if (rank >= 0 && rank < bestRank) {
                    best = i;
                    bestRank = rank;
                }
            }
            if (best < 0) break;
            symbols.values[best] = mergeResults[bestRank];
            symbols.remove(best + 1);
        }
        for (int i = 0; i < symbols.size; i++) ids.add(symbols.values[i]);
    }

    /// The `<0xXX>` tokens for the character's utf-8 bytes, or null if any one is missing.
    private int @Nullable [] byteFallback(int cp) {
        if (cp >= Character.MIN_SURROGATE && cp <= Character.MAX_SURROGATE) return null;
        var encoded = Character.toString(cp).getBytes(StandardCharsets.UTF_8);
        var out = new int[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            out[i] = bytes[encoded[i] & 0xff];
            if (out[i] < 0) return null;
        }
        return out;
    }

    private int rank(int left, int right) {
        int i = Arrays.binarySearch(mergeKeys, pair(left, right));
        return i < 0 ? -1 : mergeRanks[i];
    }

    private static long pair(int left, int right) {
        return (long) left << 32 | (right & 0xffffffffL);
    }

    /// Rust's `char::is_whitespace`, the unicode White_Space property, which is what lstrip
    /// strips. Java's isWhitespace leaves out the no-break spaces.
    private static boolean isWhitespace(int cp) {
        return (cp >= 0x09 && cp <= 0x0d)
            || cp == 0x20
            || cp == 0x85
            || cp == 0xa0
            || cp == 0x1680
            || (cp >= 0x2000 && cp <= 0x200a)
            || cp == 0x2028
            || cp == 0x2029
            || cp == 0x202f
            || cp == 0x205f
            || cp == 0x3000;
    }

    private static final class IntList {
        int[] values = new int[16];
        int size;

        void add(int value) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = value;
        }

        void remove(int index) {
            System.arraycopy(values, index + 1, values, index, size - index - 1);
            size--;
        }
    }
}
