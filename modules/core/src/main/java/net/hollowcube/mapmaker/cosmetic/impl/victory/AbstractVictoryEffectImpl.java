package net.hollowcube.mapmaker.cosmetic.impl.victory;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.impl.CosmeticImpl;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NonBlocking;

public abstract class AbstractVictoryEffectImpl extends CosmeticImpl {
    public AbstractVictoryEffectImpl(Cosmetic cosmetic) {
        super(cosmetic);
    }

    @NonBlocking
    public abstract void trigger(Player player, Point position);

}
