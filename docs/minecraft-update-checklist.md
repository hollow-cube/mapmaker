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

### The proxy (`bin/proxy-plugin`)

* Bump velocity: the pinned build, url and sha256 at the bottom of `bin/proxy-plugin/build.gradle.kts`
  (from https://fill.papermc.io/v3/projects/velocity/versions/<version>/builds/latest), and
  `velocity` in `gradle/libs.versions.toml` if the version line changed. `config-version` in
  `bin/proxy-plugin/proxy/velocity.toml` has to match what the new jar expects.
* Update [viaversion](https://hangar.papermc.io/ViaVersion/ViaVersion) and
  [viabackwards](https://hangar.papermc.io/ViaVersion/ViaBackwards) in `bin/proxy-plugin/proxy/plugins`.
* Set `velocity-servers.default` in `bin/proxy-plugin/proxy/plugins/viaversion/config.yml` to the
  latest protocol version.
* `./gradlew :bin:proxy-plugin:runProxy` runs exactly what ships, in front of a local dev server.

The proxy deploys on every push to `main` that touches it (`.github/workflows/proxy.yml`), and the
rollout is graceful: the new proxy takes new logins and the old one stays up until its last player
leaves, so a version bump only needs the servers deployed first (or at the same time) for the
players who reconnect to have somewhere to go.
