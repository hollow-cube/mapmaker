package net.hollowcube.terraform.util;

@FunctionalInterface
public interface PosPredicate {
    boolean test(int x, int y, int z);
}
