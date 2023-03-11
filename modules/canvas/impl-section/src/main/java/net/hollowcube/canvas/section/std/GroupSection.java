package net.hollowcube.canvas.section.std;

import net.hollowcube.canvas.section.ParentSection;
import net.hollowcube.canvas.section.SectionLike;
import org.jetbrains.annotations.NotNull;

public class GroupSection extends ParentSection {
    public GroupSection(int width, int height) {
        super(width, height);
    }

    @Override
    public <C extends SectionLike> C add(int x, int y, @NotNull C comp) {
        return super.add(x, y, comp);
    }

    @Override
    public <C extends SectionLike> C add(int index, @NotNull C comp) {
        return super.add(index, comp);
    }
}
