package net.hollowcube.mapmaker.map.entity.potion;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PotionEffectList implements Iterable<PotionEffectList.Entry> {
    public static final DecimalFormat DURATION_NUM_FORMAT = new DecimalFormat("#.###");

    public static final Codec<PotionEffectList> CODEC = Entry.CODEC.list().optional(List.of())
            .transform(PotionEffectList::new, PotionEffectList::entries);

    public static final int MIN_DURATION_MS = 0;
    public static final int MAX_DURATION_MS = 1000 * 60 * 60 * 24; // 24h

    private final List<Entry> entries;

    public PotionEffectList(@Nullable List<Entry> entries) {
        this.entries = new ArrayList<>(Objects.requireNonNullElse(entries, List.of()));
    }

    public PotionEffectList() {
        this(List.of());
    }

    public List<Entry> entries() {
        return entries;
    }

    @Override
    public Iterator<Entry> iterator() {
        return entries.iterator();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean isFull() {
        return entries.size() == PotionInfo.values().size();
    }

    public Entry getOrCreate(PotionInfo type) {
        var existing = get(type);
        if (existing == null) {
            existing = new Entry(type, 1, 0);
            entries.add(existing);
            Collections.sort(entries);
        }
        return existing;
    }

    public @Nullable Entry get(PotionInfo type) {
        Check.notNull(type, "type");
        for (var entry : entries) {
            if (entry.type().equals(type)) {
                return entry;
            }
        }
        return null;
    }

    public void remove(PotionInfo type) {
        var iter = entries.iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (entry.type().equals(type)) {
                iter.remove();
                return;
            }
        }
    }

    public void clear() {
        entries.clear();
    }

    public boolean has(PotionInfo type) {
        Check.notNull(type, "type");
        for (var entry : entries) {
            if (entry.type().equals(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return entries.toString();
    }

    public PotionEffectList copy() {
        var entries = new ArrayList<Entry>();
        for (var entry : this.entries) {
            entries.add(new Entry(entry.type(), entry.level(), entry.duration()));
        }
        return new PotionEffectList(entries);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class Entry implements Comparable<Entry> {
        private static final Comparator<Entry> COMPARATOR = Comparator.comparing(Entry::type);

        public static final StructCodec<Entry> CODEC = StructCodec.struct(
                "type", PotionInfo.CODEC, Entry::type,
                // Historically people were allowed to have effects > 128, need to clamp it so that you dont crash.
                "level", ExtraCodecs.clamppedInt(0, 128).optional(0), Entry::level,
                "duration", Codec.INT.optional(0), Entry::duration,
                Entry::new);

        private final PotionInfo type;
        private int level;
        private int duration;

        public Entry(PotionInfo type, int level, int duration) {
            Check.notNull(type, "type");
            this.type = type;
            this.level = level;
            this.duration = duration;
        }

        public PotionInfo type() {
            return type;
        }

        public int level() {
            return level;
        }

        public int duration() {
            return duration;
        }

        public Component durationComponent() {
            if (duration <= 0) {
                return Component.translatable("gui.effect.potion.duration.infinite");
            } else {
                return Component.text(DURATION_NUM_FORMAT.format(duration() / 1000.0));
            }
        }

        public Component readableDurationComponent() {
            if (duration <= 0) {
                return Component.translatable("gui.effect.potion.duration.infinite");
            } else {
                long hours = TimeUnit.MILLISECONDS.toHours(duration);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
                double fractionalSeconds = (duration % 1000) / 1000.0 + seconds;

                StringBuilder formattedTime = new StringBuilder();

                if (hours > 0) {
                    formattedTime.append(hours).append("h");
                    if (minutes > 0 || fractionalSeconds > 0) {
                        formattedTime.append(" ");
                    }
                }

                if (minutes > 0) {
                    formattedTime.append(minutes).append("m");
                    if (fractionalSeconds > 0) {
                        formattedTime.append(" ");
                    }
                }

                if (fractionalSeconds > 0) { // don't show 0 seconds (duh)
                    formattedTime.append(String.format("%.3fs", fractionalSeconds));
                }

                return Component.text(formattedTime.toString());
            }
        }

        public void setLevel(int level) {
            Check.argCondition(level < 0 || level > type.maxLevel(), "level must be between 0 and " + type.maxLevel());
            this.level = level;
        }

        public void setDuration(int millis) {
            Check.argCondition(millis < 0, "duration must be positive");
            this.duration = millis;
        }

        @Override
        public int compareTo(PotionEffectList.Entry o) {
            return COMPARATOR.compare(this, o);
        }

        @Override
        public String toString() {
            return "{" +
                    "t=" + type.id() +
                    ",l=" + level +
                    ",d=" + duration +
                    '}';
        }
    }
}
