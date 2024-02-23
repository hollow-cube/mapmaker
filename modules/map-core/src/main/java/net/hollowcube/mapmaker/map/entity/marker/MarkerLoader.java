package net.hollowcube.mapmaker.map.entity.marker;

import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

public interface MarkerLoader {

    /**
     * Called to spawn a marker entity in the world. This should be used to apply any relevant logic
     *
     * <p>Generally implementations should return `false` from this method to avoid spawning the marker, and either
     * spawn the entity described by the marker, or load it, etc.</p>
     *
     * <p>Note about spawning entities: This method is called during world load meaning the world is not ticking so it
     * is never valid to block on anything which requires world ticks (such as the entity spawn future).</p>
     *
     * @param world the world the marker is being loaded into
     * @param type  the type of marker being loaded
     * @param data  the NBT data of the marker
     * @param pos   the position of the marker
     * @return true to spawn the marker entity in the world, false otherwise
     */
    boolean loadMarker(@NotNull MapWorld world, @NotNull String type, @NotNull NBTCompound data, @NotNull Pos pos);

}
