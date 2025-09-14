package net.hollowcube.mapmaker.runtime.parkour.action.impl.base;

import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.minestom.server.tag.Tag;

public interface CoordinateAction<T extends CoordinateAction<T>> extends Action {

    Tag<Object> SPC_TAG = Tag.Transient("mapmaker:spc/tag");

    RelativePos target();

    T withTarget(RelativePos target);
}
