package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.Switch;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class SwitchElement extends BaseParentWithChildrenElement implements Switch {

    private int state = 0;

    public SwitchElement(@Nullable String id, int width, int height) {
        super(id, width, height);
    }

    @Override
    public void setState(@Range(from = 0, to = Integer.MAX_VALUE) int state) {
        this.state = state;
        mountCurrentState();
    }

    @Override
    protected void mount() {
        super.mount();
        mountCurrentState();
    }

    private void mountCurrentState() {
        clear();
        var child = children.get(state); //todo unhandled potential exception here
        var section = child.section();
        mountChild(0, 0, section);
    }

    @Override
    public BaseElement clone() {
        var clone = new SwitchElement(id(), width(), height());
        for (var child : children) {
            clone.children.add(child.clone());
        }
        return clone;
    }

}
