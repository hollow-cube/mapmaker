package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

@RuntimeGson
public record TabCompleteResponse(
        @UnknownNullability List<Entry> result
) {

    @RuntimeGson
    public record Entry(String id, String username) {
    }

    @Override
    public List<Entry> result() {
        return this.result == null ? List.of() : this.result;
    }

    public List<Entry> resultSafe() {
        return this.result == null ? List.of() : this.result;
    }
}
