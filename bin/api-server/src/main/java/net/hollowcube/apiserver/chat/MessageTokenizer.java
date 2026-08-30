package net.hollowcube.apiserver.chat;

import net.hollowcube.ipc.chat.MessagePart;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/// Splits a message into the parts every server renders, once, here.
///
/// The patterns and the order they run in are the Go handler's: urls first, then `:emoji:`, then
/// `[map]`. Only [MessagePart.Raw] is ever rescanned, which is what keeps `[map]` inside a url part
/// of the url rather than a second link.
public final class MessageTokenizer {

    /// A url with or without its scheme, which is how people write them.
    private static final Pattern URL = Pattern.compile(
        "(?:https?://)?[a-zA-Z0-9@:%._+~#=-]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)");
    private static final Pattern EMOJI = Pattern.compile(":([a-zA-Z0-9\\-_]+):");
    private static final Pattern MAP = Pattern.compile("\\[map]");

    /// @param mapId the published map `[map]` stands for, or null to leave `[map]` as text — which
    ///              only happens when there is none to write, since a message that mentions one
    ///              outside a published map is rejected before it gets here
    public static List<MessagePart> tokenize(String text, @Nullable String mapId) {
        var parts = List.<MessagePart>of(new MessagePart.Raw(text));
        parts = split(parts, URL, MessagePart.Url::new);
        parts = split(parts, EMOJI, match -> new MessagePart.Emoji(match.substring(1, match.length() - 1)));
        if (mapId != null) parts = split(parts, MAP, _ -> new MessagePart.Map(mapId));
        return parts;
    }

    private static List<MessagePart> split(List<MessagePart> parts, Pattern pattern,
                                           Function<String, MessagePart> matched) {
        var out = new ArrayList<MessagePart>(parts.size());
        for (var part : parts) {
            if (part instanceof MessagePart.Raw raw) splitRaw(out, raw.text(), pattern, matched);
            else out.add(part);
        }
        return List.copyOf(out);
    }

    private static void splitRaw(List<MessagePart> out, String text, Pattern pattern,
                                 Function<String, MessagePart> matched) {
        var matcher = pattern.matcher(text);
        if (!matcher.find()) {
            out.add(new MessagePart.Raw(text));
            return;
        }
        if (matcher.start() > 0) out.add(new MessagePart.Raw(text.substring(0, matcher.start())));
        out.add(matched.apply(matcher.group()));
        if (matcher.end() < text.length()) splitRaw(out, text.substring(matcher.end()), pattern, matched);
    }

    private MessageTokenizer() {
    }
}
