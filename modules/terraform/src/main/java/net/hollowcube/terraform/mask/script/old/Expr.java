package net.hollowcube.terraform.mask.script.old;

import net.minestom.server.instance.block.Block;

import java.util.List;
import java.util.Objects;

public interface Expr {
    Expr getChildAt(int index);

    List<String> complete(int index);

    int start();

    int end();

    record PlaceholderRoot(int start, int end) implements Expr {
        @Override
        public Expr getChildAt(int index) {
            return this;
        }

        @Override
        public List<String> complete(int index) {
            return List.of("!", "#", "(", ")");
        }
    }

    record BlockState(int start, int end, String value) implements Expr {
        @Override
        public Expr getChildAt(int index) {
            return this;
        }

        @Override
        public List<String> complete(int index) {
            var search = value.substring(0, index - start);
            System.out.println("completing for " + search + " " + index + " " + this);
            return Block.values().stream()
                    .map(Block::namespace)
                    .map(s -> {
                        var str = s.asString();
                        if (str.startsWith(search))
                            return str;
                        if (s.path().startsWith(search))
                            return s.path();
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .sorted()
                    .limit(5)
                    .toList();
        }
    }

    record Not(int start, Expr child) implements Expr {

        @Override
        public int end() {
            return child.end();
        }

        @Override
        public Expr getChildAt(int index) {
            if (index == start)
                return this;
            return child.getChildAt(index);
        }

        @Override
        public List<String> complete(int index) {
            return null;
        }
    }

    record Binary(Expr lhs, Expr rhs) implements Expr {

        @Override
        public int start() {
            return lhs.start();
        }

        @Override
        public int end() {
            return lhs.end();
        }

        @Override
        public Expr getChildAt(int index) {
            if (index <= lhs.end())
                return lhs.getChildAt(index);
            if (index >= rhs.start())
                return rhs.getChildAt(index);
            return this;
        }

        @Override
        public List<String> complete(int index) {
            return null;
        }
    }

}
