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
public final class JavadocTagParser {

    private static final Pattern TAG_PARAM = Pattern.compile("^@luaParam\\s+(\\w+)(\\?)?\\s+(.+?)\\s*$");
    private static final Pattern TAG_RETURN = Pattern.compile("^@luaReturn\\s+(.+?)\\s*$");
    private static final Pattern TAG_GENERIC = Pattern.compile("^@luaGeneric\\s+(\\w+)(\\.\\.\\.)?\\s*$");
    private static final Pattern LUA_TAG_PREFIX = Pattern.compile("^@lua\\w*");

    private JavadocTagParser() {
    }

    public static MemberDocs parse(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return MemberDocs.empty();

        var generics = new ArrayList<TagGeneric>();
        var params = new ArrayList<TagParam>();
        var returns = new ArrayList<String>();
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
                    params.add(new TagParam(m.group(1), m.group(2) != null, m.group(3).strip()));
                } else if ((m = TAG_RETURN.matcher(trimmed)).matches()) {
                    returns.add(m.group(1).strip());
                } else if ((m = TAG_GENERIC.matcher(trimmed)).matches()) {
                    generics.add(new TagGeneric(m.group(1), m.group(2) != null));
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

    private static String collapseDescription(List<String> lines) {
        int start = 0;
        int end = lines.size();
        while (start < end && lines.get(start).isBlank()) start++;
        while (end > start && lines.get(end - 1).isBlank()) end--;
        if (start == end) return "";
        return String.join("\n", lines.subList(start, end));
    }
}
