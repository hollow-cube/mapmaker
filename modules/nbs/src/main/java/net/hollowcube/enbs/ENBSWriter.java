package net.hollowcube.enbs;

import net.hollowcube.nbs.NBSWriter;
import net.minestom.server.network.NetworkBuffer;

public interface ENBSWriter {
    byte CURRENT_VERSION = 5;
    byte[] ENBS_MAGIC = new byte[]{0x45, 0x4E, 0x42, 0x53}; // "ENBS" in ASCII

    static ENBSWriter writer() {
        return Impl.INSTANCE;
    }

    default byte[] write(ExtendedNoteBlockSong song) {
        return NetworkBuffer.makeArray(buffer -> write(buffer, song));
    }

    void write(NetworkBuffer buffer, ExtendedNoteBlockSong song);

    final class Impl implements ENBSWriter {
        private static final ENBSWriter INSTANCE = new Impl();

        private Impl() {
        }

        @Override
        public void write(NetworkBuffer buffer, ExtendedNoteBlockSong song) {
            for (byte b : ENBS_MAGIC) {
                buffer.write(ENBSTypes.BYTE, b);
            }
            buffer.write(ENBSTypes.BYTE, CURRENT_VERSION);
            buffer.write(ENBSTypes.METADATA, song.metadata());
            NBSWriter.writer().write(buffer, song.song());
        }
    }
}
