package net.hollowcube.mapmaker.map.entity.object;

import net.minestom.server.entity.Player;

public interface ObjectEntityEditor {

    /// @return true if the edit was performed (eg gui opened), false if not (will open axiom editor)
    boolean onPlayerEdit(Player player, ObjectEntity entity);

}
