package net.hollowcube.mapmaker.panels;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RadioSelectTest {

    @Test
    void optionsCanBeRebuiltWithoutGrowingPastTheSelectorBounds() {
        var select = new RadioSelect<String>(4, 1, "normal");

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 3; i++) {
                select.clear();
                select.addOption("normal", (_, _) -> {
                });
                select.addUnselectableOption(button());
                select.addUnselectableOption(button());
                select.addUnselectableOption(button());
            }
        });
    }

    @Test
    void addingMoreOptionsThanTheSelectorCanHoldFailsImmediately() {
        var select = new RadioSelect<String>(2, 1);
        select.addUnselectableOption(button());
        select.addUnselectableOption(button());

        assertThrows(IllegalStateException.class, () -> select.addUnselectableOption(button()));
    }

    private static Button button() {
        return new Button(null, 1, 1);
    }
}
