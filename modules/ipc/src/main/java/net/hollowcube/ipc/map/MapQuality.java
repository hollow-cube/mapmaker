package net.hollowcube.ipc.map;

/// Stored by ordinal, so only ever appended to.
public enum MapQuality {
    UNRATED,
    GOOD,
    GREAT,
    EXCELLENT,
    OUTSTANDING,
    MASTERPIECE,
    UNKNOWN,
    ;

    public static MapQuality fromId(int id) {
        return id >= 0 && id < UNKNOWN.ordinal() ? values()[id] : UNRATED;
    }
}
