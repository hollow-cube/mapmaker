package net.hollowcube.terraform.session.history;

import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.session.LocalSession;
import org.jetbrains.annotations.NotNull;

public interface Change {

    static @NotNull Change of(@NotNull BlockBuffer undo, @NotNull BlockBuffer redo) {
        return new Change() {
            @Override
            public void undo(@NotNull LocalSession session) {
                session.buildTask("undo")
                        .metadata() //todo
                        .buffer(undo)
                        .ephemeral()
                       .submitForce();
            }

            @Override
            public void redo(@NotNull LocalSession session) {
                session.buildTask("redo")
                        .metadata()
                        .buffer(redo)
                        .ephemeral()
                       .submitForce();
            }
        };
    }

    void undo(@NotNull LocalSession session);

    void redo(@NotNull LocalSession session);

}
