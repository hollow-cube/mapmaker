package net.hollowcube.ipc.map;

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
