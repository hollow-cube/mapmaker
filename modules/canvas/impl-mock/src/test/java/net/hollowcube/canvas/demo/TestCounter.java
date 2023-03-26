package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.mock.MockController;
import net.hollowcube.canvas.mock.MockLabel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCounter {

    @Test
    void testDefaultState() {
        var mock = new MockController();
        var counter = mock.show(Counter::new);

        var label = mock.select("#count", MockLabel.class);
        assertEquals("0", label.argAsString(0));
    }

}
