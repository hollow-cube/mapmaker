package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.internal.standalone.trait.DepthAware;
import net.hollowcube.canvas.section.RootSection;
import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RootElement extends BoxElement {

    private record SpriteInfo(BaseElement owner, Sprite sprite, int offset) { }
    private final List<SpriteInfo> sprites = new ArrayList<>();

    private Runnable mountHandler = null;

    public RootElement(@Nullable String id, int width, int height, @NotNull Align align) {
        super(id, width, height, align);
    }

    public void addMountHandler(@NotNull Runnable runnable) {
        mountHandler = runnable;
    }

    public void addSprite(@NotNull BaseElement owner, @NotNull Sprite sprite, int offset) {
        sprites.add(new SpriteInfo(owner, sprite, 0));
        updateTitle();
    }

    public void removeSprites(@NotNull BaseElement owner) {
        sprites.removeIf(info -> info.owner == owner);
        updateTitle();
    }

    @Override
    protected void mount() {
        super.mount();
        if (mountHandler != null) {
            mountHandler.run();
        }
    }

    private void updateTitle() {
        var sprites = this.sprites.stream().sorted(Comparator.comparingInt(a -> {
            if (a.owner instanceof DepthAware depthAware) {
                return depthAware.zIndex();
            }
            return 0;
        })).toList();

        var sb = new StringBuilder();
        for (var sprite : sprites) {
            sb.append(FontUtil.computeOffset(sprite.sprite.offsetX()));
            sb.append(sprite.sprite.fontChar());
            sb.append(FontUtil.computeOffset(-(sprite.sprite.offsetX() + sprite.sprite.width())));
        }

        find(RootSection.class).setTitle(Component.text(sb.toString(), NamedTextColor.WHITE));
    }
}
