package net.hollowcube.mapmaker.map;

public enum ReportCategory {
    CHEATED,
    DISCRIMINATION,
    EXPLICIT_CONTENT,
    SPAM,
    DCMA,
    TROLL, // Not actually used
    UNPLAYABLE(true),
    ;

    private final boolean requiresComment;

    ReportCategory() {
        this(false);
    }

    ReportCategory(boolean requiresComment) {
        this.requiresComment = requiresComment;
    }

    public boolean requiresComment() {
        return requiresComment;
    }
}
