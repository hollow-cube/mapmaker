package net.hollowcube.mapmaker.runtime.parkour.action.impl.base;

import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.kyori.adventure.key.Key;
import net.minestom.server.tag.Tag;

public interface CoordinateAction<T extends CoordinateAction<T>> extends Action {

    Tag<Object> SPC_TARGET_TAG = Tag.Transient("mapmaker:spc/target_tag");
    Tag<Pair<Key, Object>> SPC_EDIT_TAG = Tag.Transient("mapmaker:spc/edit_tag");

    RelativePos target();

    T withTarget(RelativePos target);
}
