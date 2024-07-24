package net.hollowcube.luau.ap.tree;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.hollowcube.luau.ap.util.DocContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LuaDocBuilder {

    public static @NotNull JsonObject buildDocObject(@NotNull Node.Type typeNode) {
        var obj = new JsonObject();
        typeNode.accept(new Impl(), obj);
        return obj;
    }

    static class Impl implements Visitor<JsonObject> {
        private String typeName;

        private String currentName;
        private JsonArray typeKeys; // Root keys array for the type

        private boolean isOverload = false;

        @Override
        public void visitType(Node.@NotNull Type node, JsonObject output) {
            typeName = node.name();

            var typeDoc = addDocKeys(new JsonObject(), node.doc());
            currentName = "@roblox/globaltype/" + node.name();
            output.add(currentName, typeDoc);

            typeKeys = new JsonArray();
            typeDoc.add("keys", typeKeys);

            Visitor.super.visitType(node, output);
        }

        @Override
        public void visitProperty(Node.@NotNull Property node, JsonObject output) {
            var propDoc = addDocKeys(new JsonObject(), node.doc());
            var name = currentName + "." + node.name();
            output.add(name, propDoc);
            typeKeys.add(name);
        }

        @Override
        public void visitMethodOverload(Node.Method.@NotNull Overload node, JsonObject output) {
            var baseName = currentName + "." + node.name();

            var doc = new JsonObject();
            typeKeys.add(baseName);
            output.add(baseName, doc);
            var overloads = new JsonObject();
            doc.add("overloads", overloads);

            var beforeName = currentName;
            var beforeTypeKeys = typeKeys;
            isOverload = true;

            for (var actual : node.overloads()) {
                var sig = createAnonymousLuaSig(actual);
                actual.accept(this, output);
                currentName = baseName + "/overload/" + sig;
                overloads.addProperty(sig, currentName);
            }

            isOverload = false;
            currentName = beforeName;
            typeKeys = beforeTypeKeys;
        }

        @Override
        public void visitMethodActual(Node.Method.@NotNull Actual node, JsonObject output) {
            if (node.isDirect()) return; //todo support for direct

            var methDoc = addDocKeys(new JsonObject(), node.doc());
            var name = isOverload ? currentName : currentName + "." + node.name();
            output.add(name, methDoc);
            typeKeys.add(name);

            var params = new JsonArray();
            methDoc.add("params", params);
            {   // Add the self param
                var paramName = name + "/param/0";
                var paramObj = new JsonObject();
                paramObj.addProperty("name", "self");
                paramObj.addProperty("documentation", paramName);
                params.add(paramObj);
                output.add(paramName, addDocKeys(new JsonObject(), null));
            }
            for (int i = 0; i < node.args().size(); i++) {
                var arg = node.args().get(i);
                var paramName = name + "/param/" + (i + 1);

                // Object for the param array
                var paramObj = new JsonObject();
                paramObj.addProperty("name", arg.name());
                paramObj.addProperty("documentation", paramName);
                params.add(paramObj);

                // (Empty for now) global definition
                output.add(paramName, addDocKeys(new JsonObject(), null));
            }

            var returns = new JsonArray();
            methDoc.add("returns", returns);
            if (node.ret() != null) {
                var retName = currentName + "/return/0";
                returns.add(retName);
                output.add(retName, addDocKeys(new JsonObject(), null));
            }
        }

        private @NotNull JsonObject addDocKeys(@NotNull JsonObject doc, @Nullable DocContent content) {
            if (content == null) {
                // Always add an empty documentation key. Seems required.
                doc.addProperty("documentation", "");
                return doc;
            }

            doc.addProperty("documentation", content.text());
            if (content.codeSample() != null)
                doc.addProperty("code_sample", content.codeSample());
            if (content.url() != null)
                doc.addProperty("learn_more_link", content.url());
            return doc;
        }

        private @NotNull String createAnonymousLuaSig(@NotNull Node.Method.Actual meth) {
            var sig = new StringBuilder();
            sig.append("(").append(typeName);
            for (var arg : meth.args())
                sig.append(", ").append(arg.type().luaType());
            sig.append(") -> ");
            if (meth.ret() != null) {
                sig.append(meth.ret().luaType());
            } else {
                sig.append("()");
            }
            return sig.toString();
        }

    }

}
