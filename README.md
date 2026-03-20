# Map Maker

The server code behind Hollow Cube's Map Maker — a Minecraft server for creative map building. This repo contains the
Java server modules, build tooling, and resource pack assets that
power [play.hollowcube.net](https://play.hollowcube.net).

For more about Hollow Cube, visit [hollowcube.net](https://hollowcube.net).

## Project Structure

- `bin/` - Server binaries
    - `config/` - Shared config across all binaries
    - `development/` - Merged hub + playing + editor server for local development
    - `hub/` - Standalone hub server
    - `local/` - Local server distribution (future use for scripting)
    - `map/` - Standalone multi-map server (hosts multiple build, verification, or playing maps in one server)
    - `map-isolate/` - Single map server, built with native-image for playing maps
    - `packer/` - Resource pack builder
    - `proxy-plugin/` - Velocity plugin for multi-server deployments in production
    - `world-converter/` - World format conversion utilities
- `modules/` - Server modules (some linked as Git submodules)
    - `canvas/` - Deprecated GUI/inventory interface builder
    - `command/` - Command library
    - `common/` - General utilities unrelated to mapmaker/hollowcube
    - `compat/` - Client mod compatibility logic
    - `core/` - Core server logic and utilities
    - `datafix/` - Datafixers for vanilla game data
    - `map-core/` - Map world loading/management logic & some core commands
    - `map-core-test/` - Work in progress test library for map server logic
    - `map-editor/` - Map editor
    - `map-runtime/` - Parkour, build, and work in progress scripting runtime
    - `nbs/` - Work in progress Noteblock Studio player
    - `replay/` - Heavily work in progress replay recorder and player
    - `terraform/` - WorldEdit-like map editing tools
- `tools/` - Build tooling
    - `lua-slopgen` - Annotation processor to generate glue code for exposing Luau APIs from Java
    - `native-image-helper` - Native image plugin to automatically generate some reflection metadata
- `resources/` - Resource pack assets

The game servers use [Minestom](https://github.com/Minestom/Minestom) and are stateless beyond active maps, delegating
to the [api-server](https://github.com/hollow-cube/api-server) for storage and some processing.

## Getting Started

See [Development Setup](.github/DEVELOPMENT_SETUP.md) for instructions on building and running the project locally.

## Contributing

Please read [CONTRIBUTING.md](.github/CONTRIBUTING.md) before opening a pull request.

All contributors must sign
our [Contributor License Agreement](https://hollowcube.net/legal/individual-contributor-license-agreement).
You'll be prompted automatically on your first PR.

## Community

We have a dedicated `#general-dev` channel in our [Discord](https://discord.hollowcube.net) for related questions.

## License

All code including but not limited to `bin/`, `modules/`, and `tools/` is licensed under the [MIT License](LICENSE).

Art assets in `resources/` are licensed under [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/).
