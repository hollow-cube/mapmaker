package net.hollowcube.enbs;

import net.hollowcube.nbs.NBSReader;
import net.minestom.server.network.NetworkBuffer;

import java.util.Map;

public interface ENBSReader {
    byte MIN_VERSION = 1; // The min supported version (inclusive)
    byte MAX_VERSION = 1; // The max supported version (inclusive)
    byte[] ENBS_MAGIC = new byte[]{0x45, 0x4E, 0x42, 0x53}; // "ENBS" in ASCII

    static ENBSReader reader() {
        return Impl.INSTANCE;
    }

    /**
     * <p>Read a {@link ExtendedNoteBlockSong} from the given bytes in NBS format.</p>
     *
     * @apiNote This method will automatically detect whether the given bytes are in ENBS or NBS format.
     * @param bytes the bytes to read from
     * @return the read song
     *
     * @throws IllegalArgumentException      if the bytes are not a valid NBS file
     * @throws UnsupportedOperationException if the NBS version is not supported
     */
    default ExtendedNoteBlockSong read(byte[] bytes) {
        return read(NetworkBuffer.wrap(bytes, 0, bytes.length));
    }

    /**
     * <p>Read a {@link ExtendedNoteBlockSong} from the given bytes in NBS format.</p>
     *
     * @apiNote This method will automatically detect whether the given bytes are in ENBS or NBS format.
     * @param buffer the buffer to read from
     * @return the read song
     *
     * @throws IllegalArgumentException      if the bytes are not a valid NBS file
     * @throws UnsupportedOperationException if the NBS version is not supported
     */
    ExtendedNoteBlockSong read(NetworkBuffer buffer);

    final class Impl implements ENBSReader {
        private static final ENBSReader INSTANCE = new Impl();

        private Impl() {
        }

        @Override
        public ExtendedNoteBlockSong read(NetworkBuffer buffer) {
            var isEnbs = true;
            for (byte number : ENBS_MAGIC) {
                if (buffer.read(ENBSTypes.BYTE) != number) {
                    isEnbs = false;
                    break;
                }
            }

            if (!isEnbs) {
                buffer.readIndex(0);
                var song = NBSReader.reader().read(buffer);
                return new ExtendedNoteBlockSong(MAX_VERSION, song, Map.of());
            } else {
                int version = buffer.read(ENBSTypes.INT);
                if (version < MIN_VERSION || version > MAX_VERSION) {
                    throw new UnsupportedOperationException("Unsupported ENBS version: " + version);
                }

                var metadata = buffer.read(ENBSTypes.METADATA);
                var song = NBSReader.reader().read(buffer);

                return new ExtendedNoteBlockSong(version, song, Map.copyOf(metadata));
            }
        }
    }
}
