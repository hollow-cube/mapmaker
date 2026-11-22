package net.hollowcube.mapmaker.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/// Word index over ids like `entity.zombie_villager.ambient` or `oak_planks`. Every query term must match some
/// word of an id, ranked whole `.` segment > word > word prefix > substring > single typo or shared 5 char stem,
/// then shortest id.
///
/// Ids share a small vocabulary (~760 distinct words across ~2000 sound ids), so terms are matched against the
/// vocabulary and scores fan out to ids through posting lists.
public final class TokenIndex<T> {
    private static final Pattern WORD_SEPARATOR = Pattern.compile("[^a-z0-9]+");

    private static final int SCORE_SEGMENT = 5;
    private static final int SCORE_WORD = 4;
    private static final int SCORE_PREFIX = 3;
    private static final int SCORE_SUBSTRING = 2;
    private static final int SCORE_FUZZY = 1;

    private final List<T> values;
    private final int idCount;
    // Rank tiebreak per id (shorter, then alphabetical, is higher), unique so it also identifies the id.
    private final int[] tiebreak;
    private final int[] byTiebreak;

    // Sorted, so prefix matches are a contiguous range.
    private final String[] vocabulary;
    // Vocabulary joined with \0 so substring matching is a single indexOf scan; offsets[v] is where word v starts.
    private final String joinedVocabulary;
    private final int[] offsets;
    private final int[][] vocabularyByLength;
    // Ids where the word is a whole `.` segment, and ids where it is only part of one.
    private final int[][] segmentPostings;
    private final int[][] wordPostings;

    public TokenIndex(@NotNull Collection<T> values, @NotNull Function<T, String> idFunction) {
        this.values = List.copyOf(values);
        this.idCount = this.values.size();
        var ids = new String[this.idCount];

        record Postings(List<Integer> segment, List<Integer> word) {}
        var postings = new TreeMap<String, Postings>();
        for (int i = 0; i < this.idCount; i++) {
            var id = idFunction.apply(this.values.get(i)).toLowerCase(Locale.ROOT);
            ids[i] = id;
            for (var segment : id.split("\\.")) {
                var words = splitWords(segment);
                for (var word : words) {
                    var p = postings.computeIfAbsent(word, _ -> new Postings(new ArrayList<>(), new ArrayList<>()));
                    var list = words.length == 1 ? p.segment : p.word;
                    if (list.isEmpty() || list.getLast() != i) list.add(i);
                }
            }
        }

        this.vocabulary = postings.keySet().toArray(String[]::new);
        this.segmentPostings = new int[this.vocabulary.length][];
        this.wordPostings = new int[this.vocabulary.length][];
        int v = 0;
        for (var p : postings.values()) {
            this.segmentPostings[v] = p.segment.stream().mapToInt(Integer::intValue).toArray();
            this.wordPostings[v] = p.word.stream().mapToInt(Integer::intValue).toArray();
            v++;
        }

        var joined = new StringBuilder();
        this.offsets = new int[this.vocabulary.length];
        var byLength = new TreeMap<Integer, List<Integer>>();
        for (v = 0; v < this.vocabulary.length; v++) {
            joined.append('\0');
            this.offsets[v] = joined.length();
            joined.append(this.vocabulary[v]);
            byLength.computeIfAbsent(this.vocabulary[v].length(), _ -> new ArrayList<>()).add(v);
        }
        this.joinedVocabulary = joined.toString();
        int maxLength = byLength.isEmpty() ? 0 : byLength.lastKey();
        this.vocabularyByLength = new int[maxLength + 1][];
        for (int len = 0; len <= maxLength; len++) {
            this.vocabularyByLength[len] = byLength.getOrDefault(len, List.of()).stream()
                .mapToInt(Integer::intValue).toArray();
        }

        var order = IntStream.range(0, this.idCount).boxed()
            .sorted(Comparator.<Integer>comparingInt(i -> ids[i].length()).thenComparing(i -> ids[i]))
            .mapToInt(Integer::intValue).toArray();
        this.tiebreak = new int[this.idCount];
        this.byTiebreak = new int[this.idCount];
        for (int rank = 0; rank < this.idCount; rank++) {
            this.tiebreak[order[rank]] = this.idCount - 1 - rank;
            this.byTiebreak[this.idCount - 1 - rank] = order[rank];
        }
    }

    public @NotNull List<T> search(@NotNull String query, int limit, @NotNull Predicate<T> predicate) {
        var terms = splitWords(query.toLowerCase(Locale.ROOT));
        if (terms.length == 0 || limit <= 0) return List.of();

        var total = new int[this.idCount];
        var termScores = new int[this.idCount];
        var wordScores = new int[this.vocabulary.length];
        for (int t = 0; t < terms.length; t++) {
            scoreVocabulary(terms[t], wordScores);
            Arrays.fill(termScores, 0);
            for (int v = 0; v < wordScores.length; v++) {
                int score = wordScores[v];
                if (score == 0) continue;
                // A word matching exactly is worth one more when it is also a whole segment.
                int segmentScore = score == SCORE_WORD ? SCORE_SEGMENT : score;
                for (int i : this.segmentPostings[v]) termScores[i] = Math.max(termScores[i], segmentScore);
                for (int i : this.wordPostings[v]) termScores[i] = Math.max(termScores[i], score);
            }
            for (int i = 0; i < total.length; i++) {
                if (termScores[i] == 0 || (t > 0 && total[i] == 0)) total[i] = 0;
                else total[i] += termScores[i];
            }
        }

        // Min-heap of score << 32 | tiebreak, so the worst of the current top `limit` is evicted first.
        var heap = new long[limit];
        int size = 0;
        for (int i = 0; i < total.length; i++) {
            if (total[i] == 0) continue;
            long key = ((long) total[i] << 32) | this.tiebreak[i];
            if (size == limit && key <= heap[0]) continue;
            if (!predicate.test(this.values.get(i))) continue;
            if (size < limit) siftUp(heap, size++, key);
            else siftDown(heap, size, key);
        }

        Arrays.sort(heap, 0, size);
        var result = new ArrayList<T>(size);
        for (int k = size - 1; k >= 0; k--) {
            result.add(this.values.get(this.byTiebreak[(int) heap[k]]));
        }
        return result;
    }

    private static void siftUp(long[] heap, int index, long key) {
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (heap[parent] <= key) break;
            heap[index] = heap[parent];
            index = parent;
        }
        heap[index] = key;
    }

    // Replaces the root with key.
    private static void siftDown(long[] heap, int size, long key) {
        int index = 0;
        while (true) {
            int child = 2 * index + 1;
            if (child >= size) break;
            if (child + 1 < size && heap[child + 1] < heap[child]) child++;
            if (key <= heap[child]) break;
            heap[index] = heap[child];
            index = child;
        }
        heap[index] = key;
    }

    private void scoreVocabulary(String term, int[] wordScores) {
        Arrays.fill(wordScores, 0);

        int start = Arrays.binarySearch(this.vocabulary, term);
        if (start >= 0) wordScores[start] = SCORE_WORD;
        else start = -start - 1;
        for (int v = start; v < this.vocabulary.length && this.vocabulary[v].startsWith(term); v++) {
            if (wordScores[v] == 0) wordScores[v] = SCORE_PREFIX;
        }

        for (int pos = this.joinedVocabulary.indexOf(term); pos >= 0; ) {
            int v = Arrays.binarySearch(this.offsets, pos);
            if (v < 0) v = -v - 2;
            if (wordScores[v] == 0) wordScores[v] = SCORE_SUBSTRING;
            if (v + 1 >= this.offsets.length) break;
            pos = this.joinedVocabulary.indexOf(term, this.offsets[v + 1]);
        }

        if (term.length() < 5) return;
        var stem = term.substring(0, 5);
        int stemStart = Arrays.binarySearch(this.vocabulary, stem);
        if (stemStart < 0) stemStart = -stemStart - 1;
        for (int v = stemStart; v < this.vocabulary.length && this.vocabulary[v].startsWith(stem); v++) {
            if (wordScores[v] == 0) wordScores[v] = SCORE_FUZZY;
        }
        for (int len = term.length() - 1; len <= term.length() + 1; len++) {
            if (len >= this.vocabularyByLength.length) break;
            for (int v : this.vocabularyByLength[len]) {
                if (wordScores[v] == 0 && isOneEditAway(term, this.vocabulary[v])) wordScores[v] = SCORE_FUZZY;
            }
        }
    }

    private static String[] splitWords(String text) {
        return Arrays.stream(WORD_SEPARATOR.split(text))
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
    }

    private static boolean isOneEditAway(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 1) return false;
        int i = 0, j = 0, edits = 0;
        while (i < a.length() && j < b.length()) {
            if (a.charAt(i) == b.charAt(j)) {
                i++;
                j++;
                continue;
            }
            if (++edits > 1) return false;
            if (a.length() > b.length()) i++;
            else if (a.length() < b.length()) j++;
            else {
                i++;
                j++;
            }
        }
        return edits + (a.length() - i) + (b.length() - j) <= 1;
    }
}
