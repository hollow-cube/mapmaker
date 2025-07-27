# Minecraft Update Checklist

*Updated July 8, 2025*

A guide on how to update the server to a new Minecraft version.

### In `hollow-cube/mapmaker`

* Update mapmaker to the latest Minestom version.
* Add the new protocol version to the constants and name map in ProtocolVersions.
* Add any new Mojang data fixes to the `datafix` module.
* Increment the max resource pack version of the latest overlay in `resources/client`,
  or add a new overlay if any relevant shaders have changed.
* Update the `SUPPORTED_VERSIONS` constant in ProxyPlugin.

### In `hollow-cube/velocity-proxy`

* Update [velocity](https://papermc.io/downloads/velocity)
* Update the proxy plugin (`./gradlew :bin:proxy-plugin:build` in `hollow-cube/mapmaker`)
* Update [viaversion](https://hangar.papermc.io/ViaVersion/ViaVersion)
* Update [viabackwards](https://hangar.papermc.io/ViaVersion/ViaBackwards)
* Set the `velocity-servers.default` config option in viaversion config to the latest protocol version.

**When you commit these changes, the proxy will deploy automatically, making it impossible
to join.** You should time the proxy deployment with the server delpoyment. In the future
we may have a better way to handle version bumps (by keeping old servers and doing phased
rollout of proxies).
