package net.hollowcube.anticheat.control;

import com.google.gson.Gson;
import net.hollowcube.anticheat.log.TraceHeader;

/// Gson is how a control message is written, not part of the channel's surface, so it stays in
/// here rather than on [CaptureControl] where every caller would see it. It is the module's one
/// configured instance, so the enums a control message carries are spelled as the header spells
/// them.
final class ControlJson {

    static final Gson GSON = TraceHeader.GSON;

    private ControlJson() {}
}
