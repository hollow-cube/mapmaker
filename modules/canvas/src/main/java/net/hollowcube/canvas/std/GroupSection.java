package net.hollowcube.canvas.std;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.SectionLike;
import org.jetbrains.annotations.NotNull;

public class GroupSection extends ParentSection {
    public GroupSection(int width, int height) {
        super(width, height);
    }

    @Override
    public <C extends SectionLike> C add(int x, int y, @NotNull C comp) {
        return super.add(x, y, comp);
    }
}
