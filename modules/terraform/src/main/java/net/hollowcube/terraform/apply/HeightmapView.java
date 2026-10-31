package net.hollowcube.terraform.apply;

import net.minestom.server.instance.Chunk;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.function.Function;

/// One chunk-sized heightmap to be updated by [ApplyEngine].
///
/// Most likely you should not touch this, and its likely to be removed in the future.
@ApiStatus.Internal
public interface HeightmapView {
    Function<Chunk, Collection<HeightmapView>> MINESTOM = MinestomHeightmapView::of;

    boolean bottomUp();

    boolean matches(int stateId);

    int height(int x, int z);

    void setHeight(int x, int z, int y);
}
