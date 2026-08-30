package net.hollowcube.ipc.chat;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

/// One piece of a message the api has already tokenized, so that every server renders the same
/// split of it and none of them runs the regexes.
///
/// Written as `{"type": "raw", "text": "..."}`; a variant a server is too old to know decodes to
/// [Unknown] and renders as nothing.
public sealed interface MessagePart permits MessagePart.Raw, MessagePart.Url, MessagePart.Emoji,
    MessagePart.Map, MessagePart.Unknown {

    /// Text as the player typed it.
    @RuntimeGson
    record Raw(String text) implements MessagePart {
    }

    /// Something that matched the url pattern, still exactly as it was typed — with or without a
    /// scheme — since the renderer is what turns it into a link.
    @RuntimeGson
    record Url(String text) implements MessagePart {
    }

    /// A `:name:` emoji, without the colons. The name is whatever was written: an emoji this
    /// server does not have is rendered back as `:name:`.
    @RuntimeGson
    record Emoji(String name) implements MessagePart {
    }

    /// `[map]`, resolved to the published map the sender was in.
    @RuntimeGson
    record Map(String mapId) implements MessagePart {
    }

    /// A part written by something newer than this build. It cannot be sent back, only ignored.
    @RuntimeGson
    record Unknown(@Nullable String type) implements MessagePart {
    }
}
