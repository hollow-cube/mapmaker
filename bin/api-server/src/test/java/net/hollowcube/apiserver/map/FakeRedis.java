package net.hollowcube.apiserver.map;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.resps.Tuple;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/// The sorted sets the map services use, in memory, with the keys they deleted on the side. A
/// pipeline runs over a connection that answers each command as it is sent.
final class FakeRedis extends JedisPooled {

    private record Member(byte[] bytes) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Member member && Arrays.equals(bytes, member.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

    private static final Comparator<Map.Entry<Member, Double>> ORDER = Map.Entry.<Member, Double>comparingByValue().thenComparing(
        entry -> entry.getKey().bytes(),
        Arrays::compareUnsigned
    );

    final List<String> deleted = new CopyOnWriteArrayList<>();
    private final Map<String, Map<Member, Double>> sets = new HashMap<>();

    @Override
    public long del(String key) {
        deleted.add(key);
        return sets.remove(key) == null ? 0 : 1;
    }

    @Override
    public long del(byte[] key) {
        return del(new String(key, StandardCharsets.UTF_8));
    }

    @Override
    public long zadd(byte[] key, double score, byte[] member, ZAddParams params) {
        var arguments = new CommandArguments(Protocol.Command.ZADD);
        params.addParams(arguments);
        var flags = new ArrayList<String>();
        for (var argument : arguments)
            flags.add(new String(argument.getRaw(), StandardCharsets.UTF_8));
        return add(key, score, member, flags);
    }

    private long add(byte[] key, double score, byte[] member, List<String> flags) {
        var set = sets.computeIfAbsent(
            new String(key, StandardCharsets.UTF_8),
            _ -> new HashMap<>()
        );
        var existing = set.get(new Member(member));
        if (existing != null && flags.contains("LT") && score >= existing) return 0;
        if (existing != null && flags.contains("GT") && score <= existing) return 0;
        set.put(new Member(member), score);
        return existing == null ? 1 : 0;
    }

    @Override
    public long zadd(byte[] key, double score, byte[] member) {
        return zadd(key, score, member, ZAddParams.zAddParams());
    }

    @Override
    public long zrem(byte[] key, byte[]... members) {
        var set = sets.get(new String(key, StandardCharsets.UTF_8));
        if (set == null) return 0;
        var removed = 0;
        for (var member : members) if (set.remove(new Member(member)) != null) removed++;
        return removed;
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        var set = sets.get(new String(key, StandardCharsets.UTF_8));
        return set == null ? null : set.get(new Member(member));
    }

    @Override
    public List<Tuple> zrangeWithScores(byte[] key, long start, long stop) {
        return page(sorted(key), start, stop);
    }

    @Override
    public List<Tuple> zrevrangeWithScores(byte[] key, long start, long stop) {
        return page(sorted(key).reversed(), start, stop);
    }

    @Override
    public long zcount(byte[] key, byte[] min, byte[] max) {
        var set = sets.get(new String(key, StandardCharsets.UTF_8));
        if (set == null) return 0;
        var lower = new String(min, StandardCharsets.UTF_8);
        var upper = new String(max, StandardCharsets.UTF_8);
        return set.values().stream().filter(score -> within(score, lower, upper)).count();
    }

    @Override
    public Pipeline pipelined() {
        return new Pipeline(
            new Connection() {
                private final List<Object> replies = new ArrayList<>();

                @Override
                public void sendCommand(CommandArguments command) {
                    var arguments = new ArrayList<byte[]>();
                    for (Rawable argument : command) arguments.add(argument.getRaw());
                    var name = new String(arguments.getFirst(), StandardCharsets.UTF_8);
                    replies.add(switch (name) {
                        case "DEL" -> del(arguments.get(1));
                        case "ZCOUNT" -> zcount(
                            arguments.get(1),
                            arguments.get(2),
                            arguments.get(3)
                        );
                        // The score and member end a ZADD, so whatever sits between them and
                        // the key is the flags, which a pipelined write must honour like any other.
                        case "ZADD" -> add(
                            arguments.get(1),
                            Double.parseDouble(
                                new String(
                                    arguments.get(arguments.size() - 2),
                                    StandardCharsets.UTF_8
                                )
                            ),
                            arguments.getLast(),
                            arguments.subList(2, arguments.size() - 2)
                                .stream()
                                .map(argument -> new String(argument, StandardCharsets.UTF_8))
                                .toList()
                        );
                        default -> throw new UnsupportedOperationException(name);
                    });
                }

                @Override
                public List<Object> getMany(int count) {
                    var answered = List.copyOf(replies.subList(0, count));
                    replies.subList(0, count).clear();
                    return answered;
                }

                @Override
                protected void flush() {}

                @Override
                public void close() {}
            },
            false
        );
    }

    private static List<Tuple> page(List<Tuple> sorted, long start, long stop) {
        var from = (int) Math.min(start, sorted.size());
        var to = (int) Math.min(stop + 1, sorted.size());
        return from >= to ? List.of() : sorted.subList(from, to);
    }

    private List<Tuple> sorted(byte[] key) {
        var set = sets.get(new String(key, StandardCharsets.UTF_8));
        if (set == null) return List.of();
        return set.entrySet()
            .stream()
            .sorted(ORDER)
            .map(entry -> new Tuple(entry.getKey().bytes(), entry.getValue()))
            .toList();
    }

    private static boolean within(double score, String lower, String upper) {
        return above(score, lower) && above(-score, flip(upper));
    }

    private static boolean above(double score, String bound) {
        if (bound.equals("-inf")) return true;
        if (bound.equals("+inf")) return false;
        return bound.startsWith("(")
            ? score > Double.parseDouble(bound.substring(1))
            : score >= Double.parseDouble(bound);
    }

    /// An upper bound as the lower bound of the negated score.
    private static String flip(String bound) {
        if (bound.equals("+inf")) return "-inf";
        if (bound.equals("-inf")) return "+inf";
        return bound.startsWith("(")
            ? "(" + (-Double.parseDouble(bound.substring(1)))
            : String.valueOf(-Double.parseDouble(bound));
    }
}
