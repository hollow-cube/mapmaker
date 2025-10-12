package net.hollowcube.nbs;

import net.hollowcube.common.util.FutureUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

public class DefaultNbsPlayer implements NBSPlayer {

    private static final SoundEvent[] soundLookup = new SoundEvent[]{
            SoundEvent.BLOCK_NOTE_BLOCK_HARP,
            SoundEvent.BLOCK_NOTE_BLOCK_BASS,
            SoundEvent.BLOCK_NOTE_BLOCK_BASEDRUM,
            SoundEvent.BLOCK_NOTE_BLOCK_SNARE,
            SoundEvent.BLOCK_NOTE_BLOCK_HAT,
            SoundEvent.BLOCK_NOTE_BLOCK_GUITAR,
            SoundEvent.BLOCK_NOTE_BLOCK_FLUTE,
            SoundEvent.BLOCK_NOTE_BLOCK_BELL,
            SoundEvent.BLOCK_NOTE_BLOCK_CHIME,
            SoundEvent.BLOCK_NOTE_BLOCK_XYLOPHONE,
            SoundEvent.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE,
            SoundEvent.BLOCK_NOTE_BLOCK_COW_BELL,
            SoundEvent.BLOCK_NOTE_BLOCK_DIDGERIDOO,
            SoundEvent.BLOCK_NOTE_BLOCK_BIT,
            SoundEvent.BLOCK_NOTE_BLOCK_BANJO,
            SoundEvent.BLOCK_NOTE_BLOCK_PLING
    };
    private final NoteBlockSong song;
    private final Audience audience;
    @Nullable
    private final NoteBlockSong.Tick[] ticks;
    @Nullable
    public Thread task;
    private boolean isPlaying;
    private int position = 0;
    private int loopedAmount = 0;
    private boolean isPaused;

    public DefaultNbsPlayer(NoteBlockSong song, Audience audience) {
        this.song = song;
        this.audience = audience;
       this.ticks = new NoteBlockSong.Tick[song.songLengthTicks() + 1];
        for (var tick : this.song.ticks()) {
            this.ticks[tick.tickTime()] = tick;
        }
    }

    @Override
    public void start() {
        if (this.task != null) {
            resume();
        } else {
            this.isPlaying = true;
            this.task = FutureUtil.createVirtual(this::run);
        }
    }

    private void run() {
        while (this.isPlaying && this.position < this.ticks.length) {
            if (!this.isPaused) tick();
            FutureUtil.sleep((long) (1000 / (this.song.tempo() / 100f)));
        }
    }

    private void tick() {
        final var tick = this.ticks[this.position++];
        if (tick == null) return;
        for (final var instrument : tick) {
            if (instrument.instrument() >= soundLookup.length || instrument.instrument() < 0) {
                continue;
            }
            this.audience.playSound(Sound.sound(
                    soundLookup[instrument.instrument()],
                    Sound.Source.MASTER,
                    (instrument.layer().volume() * instrument.noteBlockVelocity()) / 100f,
                    getPitch(instrument)
            ));
        }
        loop();
    }

    private void loop() {
        if (!this.song.loop()) return;
        if (this.loopedAmount >= this.song.maxLoopCount() && this.song.maxLoopCount() != 0) return;
        if (this.position + 1 == this.song.songLengthTicks()) {
            this.position = song.loopStartTick();
            this.loopedAmount++;
        }
    }

    private float getPitch(NoteBlockSong.Instrument instrument) {
        final int key;
        if (instrument.noteBlockKey() < 33) {
            key = instrument.noteBlockKey() - 9;
        } else if (instrument.noteBlockKey() > 57) {
            key = instrument.noteBlockKey() - 57;
        } else {
            key = instrument.noteBlockKey() - 33;
        }

        return (float) (0.5 * Math.pow(2, key / 12f));
    }

    @Override
    public void stop() {
        if (this.task != null) this.isPlaying = false;
        this.task = null;
        this.position = 0;
    }

    @Override
    public void restart() {
        assertStarted();
        this.position = 0;
    }

    @Override
    public void pause() {
        assertStarted();
        this.isPaused = true;
    }

    @Override
    public void resume() {
        assertStarted();
        this.isPaused = false;
    }

    private void assertStarted() {
        if (this.task == null) throw new RuntimeException("Not started yet!");
    }
}
