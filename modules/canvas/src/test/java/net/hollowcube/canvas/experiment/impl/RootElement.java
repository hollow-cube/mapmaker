package net.hollowcube.canvas.experiment.impl;

import net.hollowcube.canvas.internal.standalone.sprite.FontUtil;
import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.section.RootSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RootElement extends AutoLayoutBox {

    private final List<SpriteInfo> sprites = new ArrayList<>();

    public RootElement(@Nullable String id, int width, int height, Align align) {
        super(id, width, height, align);
    }

    public void addSprite(@NotNull Element owner, @NotNull Sprite sprite, int offset) {
        sprites.add(new SpriteInfo(owner, sprite, 0));
        updateTitle();
    }

    public void removeSprites(@NotNull Element owner) {
        sprites.removeIf(info -> info.owner == owner);
        updateTitle();
    }

    private void updateTitle() {
        var sprites = this.sprites.stream().sorted(Comparator.comparingInt(a -> a.owner.zIndex())).toList();

        var sb = new StringBuilder();
        for (var sprite : sprites) {
            sb.append(FontUtil.computeOffset(sprite.sprite.offsetX()));
            sb.append(sprite.sprite.fontChar());
            sb.append(FontUtil.computeOffset(-(sprite.sprite.offsetX() + sprite.sprite.width())));
        }

        find(RootSection.class).setTitle(Component.text(sb.toString(), NamedTextColor.WHITE));
    }

    private record SpriteInfo(Element owner, Sprite sprite, int offset) {

    }

}
