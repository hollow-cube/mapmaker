package net.hollowcube.nbs;

import net.minestom.server.network.NetworkBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static net.hollowcube.nbs.NBSTypes.*;

public interface NBSReader {
    byte MIN_VERSION = 1; // The min supported version (inclusive)
    byte MAX_VERSION = 5; // The max supported version (inclusive)

    static NBSReader reader() {
        return Impl.INSTANCE;
    }

    /**
     * <p>Read a {@link NoteBlockSong} from the given bytes in NBS format.</p>
     *
     * @param bytes the bytes to read from
     * @return the read song
     *
     * @throws IllegalArgumentException      if the bytes are not a valid NBS file
     * @throws UnsupportedOperationException if the NBS version is not supported
     */
    default NoteBlockSong read(byte[] bytes) {
        return read(NetworkBuffer.wrap(bytes, 0, bytes.length));
    }

    /**
     * <p>Read a {@link NoteBlockSong} from the given bytes in NBS format.</p>
     *
     * @param buffer the buffer to read from
     * @return the read song
     *
     * @throws IllegalArgumentException      if the bytes are not a valid NBS file
     * @throws UnsupportedOperationException if the NBS version is not supported
     */
    NoteBlockSong read(NetworkBuffer buffer);

    final class Impl implements NBSReader {
        private static final NBSReader INSTANCE = new Impl();

        private Impl() {
        }

        @Override
        public NoteBlockSong read(NetworkBuffer buffer) {
            short oldSongLength = buffer.read(SHORT);

            int version;
            if (oldSongLength != 0) {
                version = -1;
            } else {
                version = buffer.read(BYTE);
            }
            boolean isLegacy = version == -1;
            if (!isLegacy && (version < MIN_VERSION || version > MAX_VERSION)) {
                var message = String.format("Only OpenNBS versions %d-%d are supported!", MIN_VERSION, MAX_VERSION);
                throw new UnsupportedOperationException(message);
            }

            int vanillaInstrumentCount = isLegacy ? 9 : buffer.read(BYTE);
            short songLengthTicks = isLegacy ? oldSongLength : (version >= 3 ? buffer.read(SHORT) : 0);
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

            var newLayerCount = new AtomicInteger();
            var ticks = readTicks(buffer, newLayerCount, isLegacy);
            var layers = readLayers(buffer, Math.max(newLayerCount.get(), layerCount), isLegacy);
            var customInstruments = readCustomInstruments(buffer);

            return new NoteBlockSong(
                    vanillaInstrumentCount, songLengthTicks, layerCount,
                    songName, songAuthor, songOriginalAuthor, songDescription,
                    tempo, autoSaving, autoSavingDuration, timeSignature,
                    minutesSpent, leftClicks, rightClicks, noteBlocksAdded,
                    noteBlocksRemoved, midiSchematicFileName, loop, maxLoopCount,
                    loopStartTick, ticks.stream().map((func) -> func.apply(layers)).toList(), layers, customInstruments
            );
        }

        private List<Function<List<NoteBlockSong.Layer>, NoteBlockSong.Tick>> readTicks(
                NetworkBuffer buffer,
                AtomicInteger layerCount,
                boolean isLegacy
        ) {
            var ticks = new ArrayList<Function<List<NoteBlockSong.Layer>, NoteBlockSong.Tick>>();

            int t = -1;
            short jumpsToNextTick;
            while ((jumpsToNextTick = buffer.read(SHORT)) != 0) {
                t += jumpsToNextTick;
                var instruments = new ArrayList<Function<List<NoteBlockSong.Layer>, NoteBlockSong.Instrument>>();

                int layer = -1;
                short jumpsToNextLayer;
                while ((jumpsToNextLayer = buffer.read(SHORT)) != 0) {
                    layer += jumpsToNextLayer;

                    byte instrument = buffer.read(BYTE);
                    byte noteBlockKey = buffer.read(BYTE);
                    byte noteBlockVelocity = isLegacy ? 100 : buffer.read(BYTE);
                    short noteBlockPanning = isLegacy ? 100 : buffer.read(UNSIGNED_BYTE);
                    short noteBlockPitch = isLegacy ? 0 : buffer.read(SHORT);

                    int finalLayer = layer;
                    if (finalLayer > layerCount.get()) {
                        layerCount.set(finalLayer + 1);
                    }
                    instruments.add((map) -> new NoteBlockSong.Instrument(
                            map.get(finalLayer), instrument, noteBlockKey,
                            noteBlockVelocity, noteBlockPanning, noteBlockPitch
                    ));
                }

                int finalT = t;
                ticks.add((map) -> new NoteBlockSong.Tick(finalT, instruments.stream()
                        .map((func) -> func.apply(map))
                        .toList()));
            }

            return ticks;
        }

        private List<NoteBlockSong.Layer> readLayers(NetworkBuffer buffer, int layerCount, boolean isLegacy) {
            if (buffer.readableBytes() == 0) return List.of();

            var layers = new ArrayList<NoteBlockSong.Layer>(layerCount);
            for (int i = 0; i < layerCount; i++) {
                var layerName = buffer.read(STRING);
                boolean layerLock = !isLegacy && buffer.read(BOOL);
                byte layerVolume = buffer.read(BYTE);
                short layerStereo = isLegacy ? 100 : buffer.read(UNSIGNED_BYTE);
                layers.add(new NoteBlockSong.Layer(i, layerName, layerLock, layerVolume, layerStereo));
            }

            return layers;
        }

        private List<NoteBlockSong.CustomInstrument> readCustomInstruments(NetworkBuffer buffer) {
            if (buffer.readableBytes() == 0) return List.of();
            short customInstrumentsCount = buffer.read(UNSIGNED_BYTE);

            var customInstruments = new ArrayList<NoteBlockSong.CustomInstrument>(customInstrumentsCount);
            for (int i = 0; i < customInstrumentsCount; i++) {
                var instrumentName = buffer.read(STRING);
                var soundFile = buffer.read(STRING);
                byte pitch = buffer.read(BYTE);
                boolean pressPianoKey = buffer.read(BOOL);
                customInstruments.add(
                        new NoteBlockSong.CustomInstrument(instrumentName, soundFile, pitch, pressPianoKey));
            }

            return customInstruments;
        }
    }
}
