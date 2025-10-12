package net.hollowcube.enbs;

import net.hollowcube.nbs.NoteBlockSong;

import java.util.Map;

/**
 * An extended version of a NoteBlockSong that includes metadata.
 */
public record ExtendedNoteBlockSong(
        int version,
        NoteBlockSong song,
        Map<String, String> metadata
) {

    public ExtendedNoteBlockSong(NoteBlockSong song, Map<String, String> metadata) {
        this(ENBSWriter.CURRENT_VERSION, song, metadata);
    }
}
