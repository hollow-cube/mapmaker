package net.hollowcube.terraform.history;

import net.hollowcube.terraform.instance.Schematic;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.util.schem.Rotation;
import org.jetbrains.annotations.NotNull;

public interface Change {

    static @NotNull Change of(@NotNull Schematic undo, @NotNull Schematic redo) {
        return new Change() {
            @Override
            public void undo(@NotNull LocalSession session) {
                undo.build(Rotation.NONE, null).apply(session.instance(), () -> {
                    System.out.println("DONE!!");
                });
            }

            @Override
            public void redo() {

            }
        };
    }

    void undo(@NotNull LocalSession session);

    void redo();

}
