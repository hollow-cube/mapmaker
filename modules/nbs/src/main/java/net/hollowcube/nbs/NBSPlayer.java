package net.hollowcube.nbs;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

public interface NBSPlayer {

    void play();


    final class Impl {
        private final Player player;
        private final NoteBlockSong song;
        private final int[] layerVolumes; // Layer index to volume

        private int index = 0; // Index into song.ticks()
        private int t = -1; // Current time in ticks

        public Impl(@NotNull Player player, @NotNull NoteBlockSong song) {
            this.player = player;
            this.song = song;

            layerVolumes = new int[song.layerCount()];
            for (int i = 0; i < song.layerCount(); i++) {
                if (i >= song.layers().size()) {
                    layerVolumes[i] = 100;
                    continue;
                }

                layerVolumes[i] = song.layers().get(i).volume();
            }
        }

        public void tick() {
            t++;
            var entry = song.ticks().get(index);
            if (t < entry.t()) return; // Wait for next notes

            index++;
            for (var instrument : entry.instruments()) {
                SoundEvent sound;
                if (instrument.instrument() < song.vanillaInstrumentCount()) {
                    sound = VANILLA_INSTRUMENTS[instrument.instrument()];
                } else throw new UnsupportedOperationException("todo custom instruments");

                float volume = ((int) instrument.noteBlockVelocity()) / 100f;
//                float volume = (layerVolumes[instrument.layer()] * instrument.noteBlockVelocity()) / 100f;
                float pitch = (float) Math.pow(2f, (instrument.noteBlockKey() - 45) / 12f);
//                int pitch = (((int) instrument.noteBlockKey()) * 100) + instrument.noteBlockPitch();

                player.playSound(Sound.sound(sound, Sound.Source.RECORD, volume, pitch));
            }
        }

        private static final SoundEvent[] VANILLA_INSTRUMENTS = new SoundEvent[]{
                SoundEvent.BLOCK_NOTE_BLOCK_HARP,
                SoundEvent.BLOCK_NOTE_BLOCK_BASEDRUM,
                SoundEvent.BLOCK_NOTE_BLOCK_SNARE,
                SoundEvent.BLOCK_NOTE_BLOCK_HAT,
                SoundEvent.BLOCK_NOTE_BLOCK_BASS,
                SoundEvent.BLOCK_NOTE_BLOCK_FLUTE,
                SoundEvent.BLOCK_NOTE_BLOCK_BELL,
                SoundEvent.BLOCK_NOTE_BLOCK_GUITAR,
                SoundEvent.BLOCK_NOTE_BLOCK_CHIME,
                SoundEvent.BLOCK_NOTE_BLOCK_XYLOPHONE,
                SoundEvent.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE,
                SoundEvent.BLOCK_NOTE_BLOCK_COW_BELL,
                SoundEvent.BLOCK_NOTE_BLOCK_DIDGERIDOO,
                SoundEvent.BLOCK_NOTE_BLOCK_BIT,
                SoundEvent.BLOCK_NOTE_BLOCK_BANJO,
                SoundEvent.BLOCK_NOTE_BLOCK_PLING,
        };
    }
}
