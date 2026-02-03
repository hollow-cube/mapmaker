package net.hollowcube.mapmaker.panels;

import net.hollowcube.mapmaker.panels.buttons.LockedButton;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Predicate;

public class LockableSelect<T extends @UnknownNullability Object> extends Select<T> {
    private final Predicate<T> isLocked;

    public LockableSelect(int slotWidth, T defaultValue, Predicate<T> isLocked) {
        super(slotWidth, defaultValue);
        this.isLocked = isLocked;
    }

    public void addLockableOption(T t, String translationKey, String icon, int iconX, int iconY) {
        if (this.isLocked.test(t)) {
            this.addLockedOption(translationKey + ".locked", icon, iconX, iconY);
        } else {
            this.addOption(t, translationKey, icon, iconX, iconY);
        }
    }

    public void addLockedOption(String translationKey, String icon, int iconX, int iconY) {
        add(count++, 0, new LockedButton(translationKey, 1, 1))
            .background("generic2/btn/default/1_1ex")
            .sprite(icon, iconX, iconY);
    }
}
