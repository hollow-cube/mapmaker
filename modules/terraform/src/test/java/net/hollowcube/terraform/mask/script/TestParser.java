package net.hollowcube.terraform.mask.script;

import org.junit.jupiter.api.Test;

public class TestParser {

    @Test
    public void workingTest() {
        var parser = new Parser("crim");
        var expr = parser.parse();
        System.out.println(expr);
        System.out.println(expr.getChildAt(4).complete(4));
    }
}
