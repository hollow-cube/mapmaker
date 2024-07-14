package net.hollowcube.mapmaker.map.script.loader;

import org.jetbrains.annotations.NotNull;

public interface MapScriptLoader {

    @NotNull ScriptManifest getManifest();

    /**
     * Returns the current bytecode of the script. It may change between calls if the loader is dynamic.
     *
     * @param id The id of the script (path probably)
     * @return The bytecode of the script
     */
    byte @NotNull [] getScriptBytecode(@NotNull String id);

}
