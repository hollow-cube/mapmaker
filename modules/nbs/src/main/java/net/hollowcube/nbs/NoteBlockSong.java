package net.hollowcube.nbs;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public record NoteBlockSong(
        int vanillaInstrumentCount,
        short songLengthTicks,
        short layerCount,
        @NotNull String name,
        @NotNull String author,
        @NotNull String originalAuthor,
        @NotNull String description,
        short tempo,
        boolean autoSaving,
        byte autoSavingDuration,
        byte timeSignature,
        int minutesSpent,
        int leftClicks,
        int rightClicks,
        int noteBlocksAdded,
        int noteBlocksRemoved,
        @NotNull String midiSchematicFileName,
        boolean loop,
        byte maxLoopCount,
        short loopStartTick,
        @NotNull List<Tick> ticks,
        @NotNull List<Layer> layers,
        @NotNull List<CustomInstrument> customInstruments
) {

    public record Instrument(
            int layer, // The vertical layer index of this instrument
            byte instrument,
            byte noteBlockKey,
            byte noteBlockVelocity,
            short noteBlockPanning,
            short noteBlockPitch
    ) {
    }

    public record Tick(
            int t, // The time of this tick (in ticks)
            @NotNull List<Instrument> instruments
    ) implements Iterable<Instrument> {

        @Override
        public @NotNull Iterator<Instrument> iterator() {
            return instruments.iterator();
        }
    }

    public record Layer(
            @NotNull String name,
            boolean locked,
            byte volume, // Percentage 0-100
            short stereo // 0-200 (0 = -2 blocks, 200 = 2 blocks)
    ) {
    }

    public record CustomInstrument(
            @NotNull String name,
            @NotNull String soundFile,
            byte soundKey,
            boolean pressPianoKey
    ) {
    }
}
