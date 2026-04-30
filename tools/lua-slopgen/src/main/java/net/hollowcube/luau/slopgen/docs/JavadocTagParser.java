package net.hollowcube.luau.slopgen.docs;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Splits the body returned by `Elements.getDocComment(Element)` into a description and the
/// recognized `@luaParam` / `@luaReturn` / `@luaGeneric` block tags.
///
/// Both traditional `/** … */` and Markdown `///` documentation comments are handled by the
/// same input shape (the JDK strips comment markers and common indentation before this code
/// runs), so the parser only sees a clean line stream.
///
/// Tags are matched against a fixed grammar; malformed `@lua…` lines are recorded as
/// [TagDiagnostic]s rather than thrown so the validator can attribute them to the correct
/// source element. Unrecognized non-`@lua` tags (`@param`, `@return`, …) are silently dropped
/// so we don't collide with javadoc tooling.
///
/// Each tag may carry a trailing description after the type, separated by ` - ` (space-dash-
/// space). Luau type expressions never contain ` - ` so the boundary is unambiguous.
///
///   `@luaParam name[?] typeExpr [- free description]`
///   `@luaReturn typeExpr [- free description]`
///   `@luaGeneric name[...] [- free description]`
public final class JavadocTagParser {

    private static final Pattern TAG_PARAM = Pattern.compile("^@luaParam\\s+(\\w+)(\\?)?\\s+(.+?)\\s*$");
    private static final Pattern TAG_RETURN = Pattern.compile("^@luaReturn\\s+(.+?)\\s*$");
    private static final Pattern TAG_GENERIC = Pattern.compile("^@luaGeneric\\s+(\\w+)(\\.\\.\\.)?(\\s+-\\s+(.+?))?\\s*$");
    private static final Pattern LUA_TAG_PREFIX = Pattern.compile("^@lua\\w*");

    private static final String DESC_SEP = " - ";

    private JavadocTagParser() {
    }

    public static MemberDocs parse(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return MemberDocs.empty();

        var generics = new ArrayList<TagGeneric>();
        var params = new ArrayList<TagParam>();
        var returns = new ArrayList<TagReturn>();
        var diagnostics = new ArrayList<TagDiagnostic>();
        var descriptionLines = new ArrayList<String>();

        boolean seenTag = false;
        for (var rawLine : raw.split("\\R", -1)) {
            var line = rawLine.stripTrailing();
            var trimmed = line.stripLeading();

            if (trimmed.startsWith("@")) {
                seenTag = true;
                Matcher m;
                if ((m = TAG_PARAM.matcher(trimmed)).matches()) {
                    var split = splitTypeAndDescription(m.group(3));
                    params.add(new TagParam(m.group(1), m.group(2) != null, split.type, split.description));
                } else if ((m = TAG_RETURN.matcher(trimmed)).matches()) {
                    var split = splitTypeAndDescription(m.group(1));
                    returns.add(new TagReturn(split.type, split.description));
                } else if ((m = TAG_GENERIC.matcher(trimmed)).matches()) {
                    var description = m.group(4) == null ? "" : m.group(4).strip();
                    generics.add(new TagGeneric(m.group(1), m.group(2) != null, description));
                } else if (LUA_TAG_PREFIX.matcher(trimmed).find()) {
                    diagnostics.add(new TagDiagnostic("Malformed slopgen tag: " + trimmed));
                }
                // Other `@…` block tags (`@param`, `@since`, …) are silently dropped: they
                // belong to standard javadoc tooling.
            } else if (!seenTag) {
                descriptionLines.add(line);
            }
            // Non-tag continuation lines after a tag has been seen are dropped (no multi-line
            // tag support yet).
        }

        return new MemberDocs(
            collapseDescription(descriptionLines),
            List.copyOf(generics),
            List.copyOf(params),
            List.copyOf(returns),
            List.copyOf(diagnostics));
    }

    /// Splits a "rest-of-line" body at the first ` - ` (space-dash-space), returning the type
    /// expression and an optional description. Luau type expressions don't include ` - ` (the
    /// arrow is `->`, not `-`), so the first occurrence is unambiguous.
    private static Split splitTypeAndDescription(String rest) {
        var trimmed = rest.strip();
        int sep = trimmed.indexOf(DESC_SEP);
        if (sep < 0) return new Split(trimmed, "");
        return new Split(trimmed.substring(0, sep).strip(), trimmed.substring(sep + DESC_SEP.length()).strip());
    }

    private record Split(String type, String description) {}

    private static String collapseDescription(List<String> lines) {
        int start = 0;
        int end = lines.size();
        while (start < end && lines.get(start).isBlank()) start++;
        while (end > start && lines.get(end - 1).isBlank()) end--;
        if (start == end) return "";
        return String.join("\n", lines.subList(start, end));
    }
}
