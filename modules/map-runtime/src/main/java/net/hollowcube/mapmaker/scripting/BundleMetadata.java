package net.hollowcube.mapmaker.scripting;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record BundleMetadata(
    int bundleFormatVersion,
    String serverRuntimeVersion,
    int luauBytecodeVersion
) {
    public static final int LATEST_VERSION = 1;

    // TODO: this should be exposed in luau-java. its a macro in the compiler module
    public static final int LUAU_BYTECODE_VERSION = 6;

    public static BundleMetadata current(int observedLuauBytecodeVersion) {
        return new BundleMetadata(
            LATEST_VERSION,
            ServerRuntime.getRuntime().version(),
            observedLuauBytecodeVersion);
    }
}
