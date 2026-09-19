package net.hollowcube.proxy;

import com.viaversion.viaversion.api.Via;

/// The only class touching ViaVersion's api, so the plugin still loads without it.
final class ViaProtocols {

    /// ViaVersion reads this when the backend connection is opened: the servers it has been told
    /// about first, then `velocity-servers` in its config, whose `default` covers an unknown one.
    static void register(String serverName, int protocolVersion) {
        var detector = Via.proxyPlatform().protocolDetectorService();
        if (protocolVersion > 0) detector.setProtocolVersion(serverName, protocolVersion);
        else detector.uncacheProtocolVersion(serverName);
    }

    private ViaProtocols() {
    }
}
