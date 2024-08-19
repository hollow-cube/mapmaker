package net.hollowcube.aj;

import com.google.gson.JsonObject;
import net.hollowcube.aj.entity.AnimationEntity;
import net.hollowcube.aj.entity.AnimationEntityTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class ModelLoader {

    public static @NotNull AnimationEntity loadModel(@NotNull JsonObject data) {
        var rootData = new JsonObject();
        rootData.addProperty("uuid", new UUID(0, 0).toString());
        rootData.addProperty("name", "root");
        var root = new AnimationEntityTypes.ItemDisplay(rootData);

        for (var nodeDataElem : data.getAsJsonObject("nodes").entrySet()) {
            var nodeData = nodeDataElem.getValue().getAsJsonObject();
            var node = AnimationEntityTypes.parse(nodeData);
            var parent = nodeData.get("parent").getAsString();
            if (parent.equals("root")) {
                root.addChild(node);
            } else {
                var parentEntity = root.findNode(UUID.fromString(parent));
                Objects.requireNonNull(parentEntity, () -> "no such parent: " + parent);
                parentEntity.addChild(node);
            }
        }

        return root;
    }
}
