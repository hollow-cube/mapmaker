package net.hollowcube.map.feature.play.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.map.entity.potion.PotionInfo;
import net.kyori.adventure.text.Component;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PotionEffectList implements Iterable<PotionEffectList.Entry> {
    public static final Codec<PotionEffectList> CODEC = Entry.CODEC.listOf()
            .xmap(PotionEffectList::new, PotionEffectList::entries);

    private final List<Entry> entries;

    public PotionEffectList(@NotNull List<Entry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    public PotionEffectList() {
        this(List.of());
    }

    public List<Entry> entries() {
        return entries;
    }

    @Override
    public @NotNull Iterator<Entry> iterator() {
        return entries.iterator();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean isFull() {
        return entries.size() == PotionInfo.values().size();
    }

    public @NotNull Entry getOrCreate(@NotNull PotionInfo type) {
        Check.notNull(type, "type");
        for (var entry : entries) {
            if (entry.type().equals(type)) {
                return entry;
            }
        }
        var entry = new Entry(type, 1, 0);
        entries.add(entry);
        Collections.sort(entries);
        return entry;
    }

    public void remove(@NotNull PotionInfo type) {
        var iter = entries.iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (entry.type().equals(type)) {
                iter.remove();
                return;
            }
        }
    }

    public boolean has(@NotNull PotionInfo type) {
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

    public static class Entry implements Comparable<Entry> {
        private static final Comparator<Entry> COMPARATOR = Comparator.comparing(Entry::type);

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                PotionInfo.CODEC.fieldOf("type").forGetter(Entry::type),
                Codec.INT.optionalFieldOf("level", 0).forGetter(Entry::level),
                Codec.INT.optionalFieldOf("duration", 0).forGetter(Entry::duration)
        ).apply(i, Entry::new));

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

        public @NotNull Component durationComponent() {
            if (duration <= 0) {
                return Component.translatable("gui.effect.potion.duration.infinite");
            } else {
                return Component.text(String.format("%.2f", duration() / 1000.0));
            }
        }

        public void setLevel(int level) {
            Check.argCondition(level < 0 || level > type.maxLevel(), "level must be between 0 and " + type.maxLevel());
            this.level = level;
        }

        public void setDuration(int duration) {
            Check.argCondition(duration < 0, "duration must be positive");
            this.duration = duration;
        }

        @Override
        public int compareTo(@NotNull PotionEffectList.Entry o) {
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
