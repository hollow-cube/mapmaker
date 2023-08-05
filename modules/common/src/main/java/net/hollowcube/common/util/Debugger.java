package net.hollowcube.common.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

public final class Debugger {
    private Debugger() {
    }

    private static final boolean ENABLED;

    static {
        //https://stackoverflow.com/a/73125047
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        List<String> args = runtime.getInputArguments();
        ENABLED = args.toString().contains("jdwp");
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void breakpoint() {
        isEnabled(); // Set a breakpoint here
    }
}
