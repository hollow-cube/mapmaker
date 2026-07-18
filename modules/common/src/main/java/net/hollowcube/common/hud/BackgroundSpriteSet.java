package net.hollowcube.common.hud;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.utils.validate.Check;

public class BackgroundSpriteSet {
    private final BadSprite left, right;
    private final BadSprite[] widthSprites = new BadSprite[9];

    public BackgroundSpriteSet(String name) {
        left = BadSprite.require(name + "/left");
        right = BadSprite.require(name + "/right");
        for (int i = 0; i < widthSprites.length; i++) {
            widthSprites[i] = BadSprite.SPRITE_MAP.get(name + "/" + (1 << i));
        }
    }

    public String build(int contentWidth, boolean includeLeft, boolean includeRight) {
        var sb = new StringBuilder();
        Check.argCondition(contentWidth > 0b111111111, "Oof too big (round 3)!");

        if (includeLeft) sb.append(left.fontChar()).append(FontUtil.computeOffset(-1));
        for (int i = 0; i < widthSprites.length; i++) {
            if ((contentWidth & (1 << i)) != 0) {
                sb.append(widthSprites[i].fontChar()).append(FontUtil.computeOffset(-1));
            }
        }
        if (includeRight) sb.append(right.fontChar());

        return sb.toString();
    }

    public String build(int contentWidth) {
        return build(contentWidth, true, true);
    }

    /// The net advance of [#build]: glyph advances (width + 1 each) minus the -1 overlap offsets.
    public int advance(int contentWidth) {
        int total = left.width() + right.width() + 1;
        for (int i = 0; i < widthSprites.length; i++) {
            if ((contentWidth & (1 << i)) != 0) total += widthSprites[i].width();
        }
        return total;
    }
}
