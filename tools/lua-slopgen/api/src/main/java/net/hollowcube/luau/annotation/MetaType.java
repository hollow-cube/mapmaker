package net.hollowcube.luau.annotation;

public enum MetaType {
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
    NAMECALL("__namecall"),
    INDEX("__index"),
    NEWINDEX("__newindex"),
    ;

    private final String methodName;

    MetaType(String methodName) {
        this.methodName = methodName;
    }

    public String methodName() {
        return this.methodName;
    }
}
