package net.hollowcube.nbs;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.hollowcube.nbs.NBSTypes.*;

public interface NBSReader {
    int MIN_VERSION = 1; // The min supported version (inclusive)
    int MAX_VERSION = 5; // The max supported version (inclusive)

    static @NotNull NBSReader nbsReader() {
        return Impl.INSTANCE;
    }

    /**
     * <p>Read a {@link NoteBlockSong} from the given bytes in NBS format.</p>
     *
     * <p>Supports reading OpenNBS versions 1-5, but NOT legacy Note Block Studio files at this time.</p>
     * TODO for myself: old format is just all the green ones missing
     *
     * @param bytes the bytes to read from
     * @return the read song
     * @throws IllegalArgumentException      if the bytes are not a valid NBS file
     * @throws UnsupportedOperationException if the NBS version is not supported
     */
    @NotNull NoteBlockSong read(byte @NotNull [] bytes);

    final class Impl implements NBSReader {
        private static final NBSReader INSTANCE = new Impl();

        private Impl() {
        }

        @Override
        public @NotNull NoteBlockSong read(byte @NotNull [] bytes) {
            var buffer = NetworkBuffer.wrap(bytes, 0, bytes.length);

            int oldSongLength = buffer.read(SHORT);
            if (oldSongLength != 0) {
                // Would be non-zero for the legacy note block studio format. Not currently supported.
                var message = String.format("Only OpenNBS versions %d-%d are supported!", MIN_VERSION, MAX_VERSION);
                throw new UnsupportedOperationException(message);
            }
            int version = buffer.read(BYTE);
            if (version < MIN_VERSION || version > MAX_VERSION) {
                var message = String.format("Only OpenNBS versions %d-%d are supported!", MIN_VERSION, MAX_VERSION);
                throw new UnsupportedOperationException(message);
            }

            int vanillaInstrumentCount = buffer.read(BYTE);
            short songLengthTicks = version >= 3 ? buffer.read(SHORT) : 0;
            short layerCount = buffer.read(SHORT);
            var songName = buffer.read(STRING);
            var songAuthor = buffer.read(STRING);
            var songOriginalAuthor = buffer.read(STRING);
            var songDescription = buffer.read(STRING);
            short tempo = buffer.read(SHORT);
            boolean autoSaving = buffer.read(BOOL);
            byte autoSavingDuration = buffer.read(BYTE);
            byte timeSignature = buffer.read(BYTE);
            int minutesSpent = buffer.read(INT);
            int leftClicks = buffer.read(INT);
            int rightClicks = buffer.read(INT);
            int noteBlocksAdded = buffer.read(INT);
            int noteBlocksRemoved = buffer.read(INT);
            var midiSchematicFileName = buffer.read(STRING);

            boolean loop = false;
            byte maxLoopCount = 0;
            short loopStartTick = 0;
            if (version >= 4) {
                loop = buffer.read(BOOL);
                maxLoopCount = buffer.read(BYTE);
                loopStartTick = buffer.read(SHORT);
            }

            var ticks = readTicks(buffer);
            var layers = readLayers(buffer, layerCount);
            var customInstruments = readCustomInstruments(buffer);

            return new NoteBlockSong(
                    vanillaInstrumentCount, songLengthTicks, layerCount,
                    songName, songAuthor, songOriginalAuthor, songDescription,
                    tempo, autoSaving, autoSavingDuration, timeSignature,
                    minutesSpent, leftClicks, rightClicks, noteBlocksAdded,
                    noteBlocksRemoved, midiSchematicFileName, loop, maxLoopCount,
                    loopStartTick, ticks, layers, customInstruments
            );
        }

        private @NotNull List<NoteBlockSong.Tick> readTicks(@NotNull NetworkBuffer buffer) {
            var ticks = new ArrayList<NoteBlockSong.Tick>();

            int t = -1;
            short jumpsToNextTick;
            while ((jumpsToNextTick = buffer.read(SHORT)) != 0) {
                t += jumpsToNextTick;
                var instruments = new ArrayList<NoteBlockSong.Instrument>();

                int layer = -1;
                short jumpsToNextLayer;
                while ((jumpsToNextLayer = buffer.read(SHORT)) != 0) {
                    layer += jumpsToNextLayer;

                    byte instrument = buffer.read(BYTE);
                    byte noteBlockKey = buffer.read(BYTE);
                    byte noteBlockVelocity = buffer.read(BYTE);
                    short noteBlockPanning = buffer.read(UNSIGNED_BYTE);
                    short noteBlockPitch = buffer.read(SHORT);

                    instruments.add(new NoteBlockSong.Instrument(
                            layer, instrument, noteBlockKey,
                            noteBlockVelocity, noteBlockPanning, noteBlockPitch
                    ));
                }

                ticks.add(new NoteBlockSong.Tick(t, instruments));
            }

            return ticks;
        }

        private @NotNull List<NoteBlockSong.Layer> readLayers(@NotNull NetworkBuffer buffer, int layerCount) {
            if (buffer.readableBytes() == 0) return List.of();

            var layers = new ArrayList<NoteBlockSong.Layer>(layerCount);
            for (int i = 0; i < layerCount; i++) {
                var layerName = buffer.read(STRING);
                boolean layerLock = buffer.read(BOOL);
                byte layerVolume = buffer.read(BYTE);
                short layerStereo = buffer.read(UNSIGNED_BYTE);
                layers.add(new NoteBlockSong.Layer(layerName, layerLock, layerVolume, layerStereo));
            }

            return layers;
        }

        private @NotNull List<NoteBlockSong.CustomInstrument> readCustomInstruments(@NotNull NetworkBuffer buffer) {
            if (buffer.readableBytes() == 0) return List.of();
            short customInstrumentsCount = buffer.read(UNSIGNED_BYTE);

            var customInstruments = new ArrayList<NoteBlockSong.CustomInstrument>(customInstrumentsCount);
            for (int i = 0; i < customInstrumentsCount; i++) {
                var instrumentName = buffer.read(STRING);
                var soundFile = buffer.read(STRING);
                byte pitch = buffer.read(BYTE);
                boolean pressPianoKey = buffer.read(BOOL);
                customInstruments.add(new NoteBlockSong.CustomInstrument(instrumentName, soundFile, pitch, pressPianoKey));
            }

            return customInstruments;
        }
    }
}
