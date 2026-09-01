package net.hollowcube.anticheat.log;

import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.Protocol776;
import net.hollowcube.anticheat.protocol.ProtocolState;

import java.io.Flushable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/// Prints a trace: the header, what the body holds, and with `--frames` every frame.
///
/// Lives in this module rather than the proxy plugin so reading a shipped trace never needs
/// Velocity, or anything else, on the classpath.
public final class Dump {

    public static void main(String[] args) {
        var files = new ArrayList<String>();
        boolean frames = false;
        for (var arg : args) {
            if (arg.isEmpty()) continue;
            if (arg.equals("--frames")) frames = true;
            else if (arg.startsWith("--")) throw new IllegalArgumentException("unknown option: " + arg);
            else files.add(arg);
        }
        if (files.isEmpty()) {
            System.err.println("usage: dump [--frames] <trace.trace>...");
            System.exit(2);
        }
        for (var file : files) dump(Path.of(file), frames, System.out);
    }

    public static void dump(Path path, boolean printFrames, Appendable out) {
        try (var reader = TraceReader.open(path)) {
            var header = reader.header();
            line(out, "file            " + path);
            line(out, "formatVersion   " + header.formatVersion());
            line(out, "dictionary      " + header.dictionaryId());
            line(out, "clientPvn        " + header.clientPvn());
            line(out, "brand           " + header.brand());
            line(out, "player          " + header.playerName() + " " + header.playerId());
            line(out, "connection      " + header.connectionId());
            line(out, "capture         " + header.captureId() + " " + header.reason()
                + " closedBy " + header.closedBy() + " cohort " + header.cohort());
            line(out, "proxy           " + header.proxy() + " " + header.proxyVersion());
            line(out, "trim            " + header.trim());
            line(out, "window          " + header.startedAt() + " .. " + header.endedAt());
            line(out, "pingIds         " + header.pingIds());
            line(out, "flags           " + header.flags());
            line(out, "counters        " + header.counters());
            if (!header.extras().isEmpty()) line(out, "extras          " + header.extras());

            line(out, "prelude         " + reader.prelude().size() + " frames");
            line(out, "chunks          " + reader.chunks().size());
            for (var chunk : reader.chunks()) {
                long inline = chunk.sections().stream()
                    .filter(section -> section instanceof WorldChunk.SectionEntry.Inline).count();
                line(out, "  " + chunk.chunkX() + " " + chunk.chunkZ() + " sections " + chunk.sections().size()
                    + " (" + inline + " inline)");
            }

            var byPacket = new LinkedHashMap<String, Counter>();
            var byDirection = new LinkedHashMap<Direction, Counter>();
            var byState = new LinkedHashMap<ProtocolState, Counter>();
            long count = 0;
            long first = 0;
            long last = 0;
            for (var frame = reader.nextFrame(); frame != null; frame = reader.nextFrame()) {
                if (count == 0) first = frame.tNs();
                last = frame.tNs();
                count++;
                byPacket.computeIfAbsent(key(header, frame), key -> new Counter()).add(frame);
                byDirection.computeIfAbsent(frame.direction(), key -> new Counter()).add(frame);
                byState.computeIfAbsent(frame.state(), key -> new Counter()).add(frame);
                if (printFrames) line(out, "  " + frame(header, frame));
            }

            line(out, "frames          " + count + (reader.truncated() ? " (TRUNCATED)" : ""));
            line(out, "span            " + first + " .. " + last + " ns (" + (last - first) / 1_000_000 + " ms)");
            for (var entry : byDirection.entrySet())
                line(out, "  " + entry.getKey() + " " + entry.getValue());
            for (var entry : byState.entrySet())
                line(out, "  " + entry.getKey() + " " + entry.getValue());
            byPacket.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().frames, a.getValue().frames))
                .forEach(entry -> line(out, "  " + entry.getKey() + " " + entry.getValue()));
        }
    }

    private static String frame(TraceHeader header, Frame frame) {
        return frame.tNs() + " " + frame.direction() + " " + frame.state() + " " + name(header, frame)
            + " (0x" + Integer.toHexString(frame.packetId()) + ") " + frame.bytes().length + "B"
            + (frame.pingId() == Frame.NO_PING ? "" : " ping 0x" + Integer.toHexString(frame.pingId()));
    }

    private static String key(TraceHeader header, Frame frame) {
        return frame.direction() + " " + frame.state() + " " + name(header, frame);
    }

    /// Only 776 has a registry today; anything else is named by its id, because a wrong name is
    /// worse than none.
    private static String name(TraceHeader header, Frame frame) {
        if (header.clientPvn() != Protocol776.PROTOCOL_VERSION) return "id:" + frame.packetId();
        return Protocol776.lookup(frame.state(), frame.direction(), frame.packetId()).name();
    }

    private static void line(Appendable out, String text) {
        try {
            out.append(text).append('\n');
            if (out instanceof Flushable flushable) flushable.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final class Counter {
        private long frames;
        private long bytes;

        void add(Frame frame) {
            frames++;
            bytes += frame.bytes().length;
        }

        @Override
        public String toString() {
            return frames + " frames, " + bytes + "B";
        }
    }

    private Dump() {}
}
