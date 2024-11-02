package net.hollowcube.mapmaker.cosmetic.impl.victory;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.impl.CosmeticImpl;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractVictoryEffectImpl extends CosmeticImpl {
    public AbstractVictoryEffectImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
    }

    @NonBlocking
    public abstract void trigger(@NotNull Player player, @NotNull Point position);

}
