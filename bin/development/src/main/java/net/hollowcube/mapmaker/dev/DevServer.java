package net.hollowcube.mapmaker.dev;

import net.hollowcube.map2.runtime.MapServerInitializer;

public class DevServer {
    public static void main(String[] args) {
        //todo need to change to use system flags for configuring runtime properties so it plays better with bazel cache.
        // The following all need to be set with args
        // --define RESOURCE_PACK_SHA=dev
        // --define MAPMAKER_UNLEASH_ENABLED=true
        // --jvmopt="-Dmapmaker.noop=false"
        // --jvmopt="-Dunleash.default=true"

        MapServerInitializer.run(DevServerRunner::new);
    }
}
