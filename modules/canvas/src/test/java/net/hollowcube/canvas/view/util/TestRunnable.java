package net.hollowcube.canvas.view.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRunnable implements Runnable {
    private boolean ran = false;

    @Override
    public void run() {
        ran = true;
    }

    public void assertRan() {
        assertTrue(ran, "Runnable was not ran");
    }

    public void assertNotRan() {
        assertFalse(ran, "Runnable was ran");
    }
}
