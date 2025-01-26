package net.hollowcube.compat.noxesium.qib;

import com.noxcrew.noxesium.api.qib.QibEffect;
import net.minestom.server.utils.validate.Check;

import java.util.List;

public class QibEffects {

    public static QibEffect of(QibEffect... effects) {
        Check.stateCondition(effects.length == 0, "Cannot create a QibEffect with no effects");
        return effects.length == 1 ? effects[0] : new QibEffect.Multiple(List.of(effects));
    }

}
