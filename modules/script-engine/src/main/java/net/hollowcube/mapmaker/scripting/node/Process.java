package net.hollowcube.mapmaker.scripting.node;

import net.hollowcube.common.ServerRuntime;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static net.hollowcube.mapmaker.scripting.util.Proxies.freezeObject;
import static net.hollowcube.mapmaker.scripting.util.Proxies.proxyObject;

/**
 * Implements a small part of the <a href="https://nodejs.org/api/process.html">node:process</a> api.
 */
public class Process {

    public static @NotNull Process sandboxedProcess() {
        var nodeEnv = ServerRuntime.getRuntime().isDevelopment() ? "development" : "production";
        return new Process(Map.of("NODE_ENV", nodeEnv));
    }

    @HostAccess.Export
    public final ProxyObject env;

    public Process(@NotNull Map<String, String> env) {
        this.env = freezeObject(proxyObject(new HashMap<>(env)));
    }

    /*
    There are some other relevant APIs we may want to include here:
    https://nodejs.org/api/process.html

    * process.features.[...] - An object containing feature detection values for what is available in the current context.
    * process.memoryUsage/availableMemory/etc - Can we get this info from graaljs? Or its just opaque?
    * process.uptime
    * process.version/versions

     */

}
