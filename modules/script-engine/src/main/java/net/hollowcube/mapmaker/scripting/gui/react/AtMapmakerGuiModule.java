package net.hollowcube.mapmaker.scripting.gui.react;

import net.hollowcube.mapmaker.scripting.cjs.Module;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public record AtMapmakerGuiModule(@NotNull Module react) {

    @HostAccess.Export
    public Value useState(Value... args) {
        return react.exports().getMember("useState").execute((Object[]) args);
    }
}
