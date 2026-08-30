package net.hollowcube.anticheat;

/// Client protocol versions the capture pipeline knows how to read and write.
public final class Protocol {

    /// 26.2, the only client version phase 0 captures.
    public static final int PVN_776 = 776;

    public static boolean isSupported(int pvn) {
        return pvn == PVN_776;
    }

    private Protocol() {}
}
