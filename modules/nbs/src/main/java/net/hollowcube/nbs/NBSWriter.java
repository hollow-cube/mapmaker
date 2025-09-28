package net.hollowcube.nbs;

import com.google.gson.JsonObject;
import net.minestom.server.network.NetworkBuffer;

import static net.hollowcube.nbs.NBSTypes.*;

public interface NBSWriter {
    byte CURRENT_VERSION = 5;

    static NBSWriter nbsWriter() {
        return NBSWriter.Impl.INSTANCE;
    }

    byte[] write(NoteBlockSong noteBlockSong);

    final class Impl implements NBSWriter {
        static final Impl INSTANCE = new Impl();

        @Override
        public byte[] write(NoteBlockSong noteBlockSong) {
            return NetworkBuffer.makeArray(buffer -> {
                buffer.write(SHORT, (short) 0);

                buffer.write(BYTE, CURRENT_VERSION);
                buffer.write(BYTE, (byte) noteBlockSong.vanillaInstrumentCount());
                buffer.write(SHORT, noteBlockSong.songLengthTicks());
                buffer.write(SHORT, noteBlockSong.layerCount());
                buffer.write(STRING, noteBlockSong.data().name());
                buffer.write(STRING, noteBlockSong.data().author());
                buffer.write(STRING, noteBlockSong.data().originalAuthor());
                buffer.write(STRING, serializeData(noteBlockSong.data()));
                buffer.write(SHORT, noteBlockSong.tempo());
                buffer.write(BOOL, noteBlockSong.autoSaving());
                buffer.write(BYTE, noteBlockSong.autoSavingDuration());
                buffer.write(BYTE, noteBlockSong.timeSignature());
                buffer.write(INT, noteBlockSong.minutesSpent());
                buffer.write(INT, noteBlockSong.leftClicks());
                buffer.write(INT, noteBlockSong.rightClicks());
                buffer.write(INT, noteBlockSong.noteBlocksAdded());
                buffer.write(INT, noteBlockSong.noteBlocksRemoved());
                buffer.write(STRING, noteBlockSong.midiSchematicFileName());
                buffer.write(BOOL, noteBlockSong.loop());
                buffer.write(BYTE, noteBlockSong.maxLoopCount());
                buffer.write(SHORT, noteBlockSong.loopStartTick());

                writeTicks(buffer, noteBlockSong);
                writeLayers(buffer, noteBlockSong);
                writeCustomInstruments(buffer, noteBlockSong);
            });
        }

        private void writeCustomInstruments(NetworkBuffer buffer, NoteBlockSong noteBlockSong) {
            buffer.write(BYTE, (byte) noteBlockSong.customInstruments().size());

            for (var customInstrument : noteBlockSong.customInstruments()) {
                buffer.write(STRING, customInstrument.name());
                buffer.write(STRING, customInstrument.soundFile());
                buffer.write(BYTE, customInstrument.soundKey());
                buffer.write(BOOL, customInstrument.pressPianoKey());
            }
        }

        private void writeLayers(NetworkBuffer buffer, NoteBlockSong noteBlockSong) {
            for (var layer : noteBlockSong.layers()) {
                buffer.write(STRING, layer.name());
                buffer.write(BOOL, layer.locked());
                buffer.write(BYTE, layer.volume());
                buffer.write(UNSIGNED_BYTE, layer.stereo());
            }
        }

        private void writeTicks(NetworkBuffer buffer, NoteBlockSong noteBlockSong) {
            int lastTick = -1;
            for (var tick : noteBlockSong.ticks()) {
                var delta = tick.tickTime() - lastTick;
                lastTick = tick.tickTime();
                buffer.write(SHORT, (short) delta);

                int layer = -1;
                for (var instruments : tick.instruments()) {
                    var layerIndex = instruments.layer().index();
                    var layerDelta = layerIndex - layer;
                    layer = layerIndex;
                    buffer.write(SHORT, (short) layerDelta);

                    buffer.write(BYTE, instruments.instrument());
                    buffer.write(BYTE, instruments.noteBlockVelocity());
                    buffer.write(UNSIGNED_BYTE, instruments.noteBlockPanning());
                    buffer.write(SHORT, instruments.noteBlockPitch());
                }
                buffer.write(SHORT, (short) 0);
            }
            buffer.write(SHORT, (short) 0);

        }

        private String serializeData(NoteBlockSong.Data data) {
            if (data.data().isEmpty() && data.link() != null) {
                return data.description();
            }

            var dataObject = new JsonObject();
            data.data().forEach(dataObject::addProperty);

            dataObject.addProperty("link", data.link());
            dataObject.addProperty("description", data.description());

            return "mapmaker:data" + dataObject.toString();
        }
    }
}
