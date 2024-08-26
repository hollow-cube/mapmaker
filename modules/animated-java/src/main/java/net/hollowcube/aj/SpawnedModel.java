package net.hollowcube.aj;

import net.minestom.server.Viewable;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;

public interface SpawnedModel extends Viewable {

    @NotNull Pos position();
    void setPosition(@NotNull Pos position);

    void play(@NotNull String animationName);
    void stop(@NotNull String animationName);
    boolean isPlaying(@NotNull String animationName);

    @NotNull String variant();
    void setVariant(@NotNull String variantName);

}
