package net.hollowcube.luau.gen;

public enum Meta {
    NONE(""), // Used to indicate no metamethod (default)

    ADD("__add"),
    SUB("__sub"),
    MUL("__mul"),
    DIV("__div"),
    UNM("__unm"),
    MOD("__mod"),
    POW("__pow"),
    IDIV("__idiv"),
    CONCAT("__concat"),

    EQ("__eq"),
    LE("__le"),
    LT("__lt"),

    LEN("__len"),
    TOSTRING("__tostring"),

    ITER("__iter"),
    CALL("__call"),

    // These have special handling already, could add fallbacks to an override
    // on the type but not implementing for now as there is no existing use case.
    // NAMECALL("__namecall"),
    // INDEX("__index"),
    // NEWINDEX("__newindex"),
    ;

    private final String methodName;

    Meta(String methodName) {
        this.methodName = methodName;
    }

    public String methodName() {
        return this.methodName;
    }
}
