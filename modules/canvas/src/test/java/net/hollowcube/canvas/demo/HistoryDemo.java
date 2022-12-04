package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class HistoryDemo extends ParentSection {

    public HistoryDemo() {
        super(9, 6);

        add(0, 0, new ButtonSection(1, 1, ItemStack.of(Material.RED_STAINED_GLASS_PANE), () -> {
            var router = find(RouterSection.class);
            if (router == null) return;
            router.push(new Page1());
        }));

        add(1, 0, new ButtonSection(1, 1, ItemStack.of(Material.GREEN_STAINED_GLASS_PANE), () -> {
            var router = find(RouterSection.class);
            if (router == null) return;
            router.push(new Page2());
        }));

        add(2, 0, new ButtonSection(1, 1, ItemStack.of(Material.BLUE_STAINED_GLASS_PANE), () -> {
            var router = find(RouterSection.class);
            if (router == null) return;
            router.push(new Page3());
        }));

        add(3, 0, new ButtonSection(1, 1, ItemStack.of(Material.BARRIER), () -> {
            var router = find(RouterSection.class);
            if (router == null) return;
            router.push(new HistoryDemo());
        }));
    }

    @Override
    protected void mount() {
        super.mount();

        if (find(RouterSection.class).hasHistory()) {
            add(0, 1, new ButtonSection(1, 1, ItemStack.of(Material.ARROW), () -> {
                var router = find(RouterSection.class);
                if (router == null) return;
                router.pop();
            }));
        }
    }

    public static class Page1 extends ParentSection {
        public Page1() {
            super(9, 6);

            add(0, 0, new ButtonSection(9, 1,
                    ItemStack.of(Material.RED_STAINED_GLASS_PANE), () -> {}));

            add(0, 1, new ButtonSection(1, 1, ItemStack.of(Material.ARROW), () -> {
                var router = find(RouterSection.class);
                if (router == null) return;
                router.pop();
            }));
        }
    }

    public static class Page2 extends ParentSection {
        public Page2() {
            super(9, 6);

            add(0, 0, new ButtonSection(9, 1,
                    ItemStack.of(Material.GREEN_STAINED_GLASS_PANE), () -> {}));

            add(0, 1, new ButtonSection(1, 1, ItemStack.of(Material.ARROW), () -> {
                var router = find(RouterSection.class);
                if (router == null) return;
                router.pop();
            }));
        }
    }

    public static class Page3 extends ParentSection {
        public Page3() {
            super(9, 2);

            add(0, 0, new ButtonSection(9, 1,
                    ItemStack.of(Material.BLUE_STAINED_GLASS_PANE), () -> {}));

            add(0, 1, new ButtonSection(1, 1, ItemStack.of(Material.ARROW), () -> {
                var router = find(RouterSection.class);
                if (router == null) return;
                router.pop();
            }));
        }
    }


}
