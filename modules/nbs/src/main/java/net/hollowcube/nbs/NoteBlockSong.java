package net.hollowcube.nbs;

import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public record NoteBlockSong(
        int vanillaInstrumentCount,
        short songLengthTicks,
        short layerCount,
        Data data,
        short tempo,
        boolean autoSaving,
        byte autoSavingDuration,
        byte timeSignature,
        int minutesSpent,
        int leftClicks,
        int rightClicks,
        int noteBlocksAdded,
        int noteBlocksRemoved,
        String midiSchematicFileName,
        boolean loop,
        byte maxLoopCount,
        short loopStartTick,
        List<Tick> ticks,
        List<Layer> layers,
        List<CustomInstrument> customInstruments
) {

    public record Instrument(
            Layer layer,
            byte instrument,
            byte noteBlockKey,
            byte noteBlockVelocity, // volume from 0-100 in percent
            short noteBlockPanning,
            short noteBlockPitch
    ) {
    }

    public record Tick(
            int tickTime, // The index this tick is at
            List<Instrument> instruments
    ) implements Iterable<Instrument> {

        @Override
        public Iterator<Instrument> iterator() {
            return instruments.iterator();
        }
    }

    public record Layer(
            int index,
            String name,
            boolean locked,
            byte volume, // Percentage 0-100
            short stereo // 0-200 (200 = two blocks left, 100 = center, 0 = two blocks right)
    ) {
    }

    public record CustomInstrument(
            String name,
            String soundFile,
            byte soundKey,
            boolean pressPianoKey
    ) {
    }

    /**
     * IMPORTANT! This class is not in the nbs spec, we encode every data in the data map into the description as json. The link also gets loaded from there.
     */
    public record Data(
            String name,
            String author,
            String originalAuthor,
            String description,
            @Nullable String link,
            Map<String, String> data
    ) {
    }
}
