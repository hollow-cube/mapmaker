package net.hollowcube.mapmaker.api.maps;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@RuntimeGson
public record MapReport(
    String reporter,
    List<Category> categories,
    @Nullable String comment,
    @Nullable Pos location,
    @Nullable JsonObject context
) {
    public enum Category {
        CHEATED,
        DISCRIMINATION,
        EXPLICIT_CONTENT,
        SPAM,
        DMCA,
        TROLL, // Not actually used
        UNPLAYABLE(true),
        ;

        private final boolean requiresComment;

        Category() {
            this(false);
        }

        Category(boolean requiresComment) {
            this.requiresComment = requiresComment;
        }

        public boolean requiresComment() {
            return requiresComment;
        }
    }
}
