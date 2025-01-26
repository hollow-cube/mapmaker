package net.hollowcube.compat.noxesium.qib;

import com.noxcrew.noxesium.api.qib.QibDefinition;
import com.noxcrew.noxesium.api.qib.QibEffect;

public class QibDefinitions {

    public static QibDefinition onEnter(QibEffect effect) {
        return new QibDefinition(effect, null, null, null, false);
    }

    public static QibDefinition onExit(QibEffect effect) {
        return new QibDefinition(null, effect, null, null, false);
    }

    public static QibDefinition onTick(QibEffect effect) {
        return new QibDefinition(null, null, effect, null, false);
    }

}
