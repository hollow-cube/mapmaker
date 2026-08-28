package net.hollowcube.sqlgen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/// Splits a query file into `-- name:` blocks and rewrites our `$identifier` placeholders into the
/// `?` JDBC wants.
///
/// This is deliberately not a SQL parser: everything the generator needs to know about the shape of
/// a query comes back from the server in [Describe]. All that happens here is finding the block
/// boundaries, the directives, and the placeholders.
final class QueryFileParser {

    private static final Pattern HEADER = Pattern.compile("^\\s*--\\s*name:\\s*(\\w+)\\s+:(\\w+)\\s*$");
    private static final Pattern DIRECTIVE = Pattern.compile("^\\s*--\\s*(nullable|not-null):\\s*(.*)$");

    static QueryFile parse(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var fileName = file.getFileName().toString();
        var group = fileName.endsWith(".sql") ? fileName.substring(0, fileName.length() - 4) : fileName;

        var queries = new ArrayList<QueryFile.Query>();
        Block block = null;
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            var header = HEADER.matcher(line);
            if (header.matches()) {
                if (block != null) queries.add(block.finish(group));
                block = new Block(header.group(1), QueryFile.Tag.parse(header.group(2)), i + 1);
                continue;
            }
            if (block == null) {
                if (line.isBlank() || line.stripLeading().startsWith("--")) continue;
                throw new GenException(file + ":" + (i + 1) + ": SQL before the first '-- name:' header");
            }
            block.add(line);
        }
        if (block != null) queries.add(block.finish(group));
        if (queries.isEmpty()) throw new GenException(file + ": no '-- name:' queries");

        return new QueryFile(group, List.copyOf(queries));
    }

    private static final class Block {
        private final String name;
        private final QueryFile.Tag tag;
        private final int line;
        private final StringBuilder sql = new StringBuilder();
        private final Set<String> nullable = new LinkedHashSet<>();
        private final Set<String> notNull = new LinkedHashSet<>();

        Block(String name, QueryFile.Tag tag, int line) {
            this.name = name;
            this.tag = tag;
            this.line = line;
        }

        void add(String text) {
            var directive = DIRECTIVE.matcher(text);
            if (directive.matches()) {
                var target = directive.group(1).equals("nullable") ? nullable : notNull;
                for (var name : directive.group(2).split(",")) {
                    var trimmed = name.trim();
                    if (!trimmed.isEmpty()) target.add(trimmed.toLowerCase(Locale.ROOT));
                }
                return;
            }
            sql.append(text).append('\n');
        }

        QueryFile.Query finish(String group) {
            var text = sql.toString().strip();
            while (text.endsWith(";")) text = text.substring(0, text.length() - 1).strip();
            if (text.isEmpty()) throw new GenException("query '" + name + "' in " + group + ".sql has no SQL");

            var rewrite = rewritePlaceholders(text, name);
            return new QueryFile.Query(name, tag, rewrite.sql(), rewrite.params(), rewrite.binds(),
                Set.copyOf(nullable), Set.copyOf(notNull), rewrite.holes(), line);
        }
    }

    private record Rewrite(String sql, List<String> params, List<Integer> binds, List<QueryFile.Hole> holes) {
    }

    /// Replaces every `$identifier` outside a string, dollar-quoted body, or comment with `?`,
    /// recording which argument each `?` binds. Repeating a placeholder binds the same argument
    /// again rather than adding one.
    private static Rewrite rewritePlaceholders(String sql, String queryName) {
        var out = new StringBuilder(sql.length());
        var params = new ArrayList<String>();
        var binds = new ArrayList<Integer>();
        var holes = new ArrayList<QueryFile.Hole>();

        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\'') {
                i = copyQuoted(sql, i, out);
            } else if (c == '-' && next(sql, i) == '-') {
                i = copyUntil(sql, i, "\n", out);
            } else if (c == '/' && next(sql, i) == '*') {
                int close = sql.indexOf("*/", i + 2);
                var kind = close < 0 ? null : QueryFile.Hole.Kind.of(collapse(sql.substring(i + 2, close)));
                if (kind == null) {
                    i = copyUntil(sql, i, "*/", out);
                    continue;
                }
                for (var hole : holes) {
                    if (hole.kind() != kind) continue;
                    throw new GenException("query '" + queryName + "' has more than one /* "
                        + kind.keyword + " */ hole");
                }
                // The marker is dropped rather than copied: with nothing filled in the statement
                // still has to describe, and an empty hole is simply an absent clause.
                holes.add(new QueryFile.Hole(kind, out.length(), binds.size()));
                i = close + 2;
            } else if (c == '$' && Character.isDigit(next(sql, i))) {
                throw new GenException("query '" + queryName + "' uses a positional placeholder '$"
                    + next(sql, i) + "'; this dialect takes named placeholders like $limit");
            } else if (c == '$' && isIdentifierStart(next(sql, i))) {
                int end = i + 1;
                while (end < sql.length() && isIdentifierPart(sql.charAt(end))) end++;
                var name = sql.substring(i + 1, end);
                if (isDollarQuote(sql, end)) {
                    i = copyUntil(sql, i, "$" + name + "$", out);
                    continue;
                }
                int index = params.indexOf(name);
                if (index < 0) {
                    index = params.size();
                    params.add(name);
                }
                binds.add(index);
                out.append('?');
                i = end;
            } else if (c == '$' && next(sql, i) == '$') {
                i = copyUntil(sql, i, "$$", out);
            } else {
                out.append(c);
                i++;
            }
        }
        return new Rewrite(out.toString(), List.copyOf(params), List.copyOf(binds), List.copyOf(holes));
    }

    /// True when the identifier that just ended at `end` is closed by another `$`, making the whole
    /// thing a dollar-quote tag (`$body$ ... $body$`) rather than a placeholder.
    private static boolean isDollarQuote(String sql, int end) {
        return end < sql.length() && sql.charAt(end) == '$';
    }

    private static int copyQuoted(String sql, int start, StringBuilder out) {
        out.append('\'');
        int i = start + 1;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            out.append(c);
            i++;
            if (c != '\'') continue;
            if (i < sql.length() && sql.charAt(i) == '\'') {
                out.append('\'');
                i++;
                continue;
            }
            return i;
        }
        return i;
    }

    /// Copies from `start` through the end of `terminator` (or the end of input) verbatim.
    private static int copyUntil(String sql, int start, String terminator, StringBuilder out) {
        int end = sql.indexOf(terminator, start + terminator.length());
        end = end < 0 ? sql.length() : end + terminator.length();
        out.append(sql, start, end);
        return end;
    }

    /// A block comment's body, whitespace-collapsed, so `/*  order   by  */` reads the same as
    /// `/* order by */`.
    private static String collapse(String text) {
        return text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static char next(String sql, int i) {
        return i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
    }

    private static boolean isIdentifierStart(char c) {
        return c == '_' || Character.isLetter(c);
    }

    private static boolean isIdentifierPart(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    private QueryFileParser() {
    }
}
