package net.hollowcube.luau.ap.tree;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import org.jetbrains.annotations.NotNull;

public class LuaTypeDefinitionBuilder {

    public static @NotNull String buildTypeDefObject(@NotNull Node.Type typeNode) {
        var builder = CodeBlock.builder();
        typeNode.accept(new Impl(), builder);
        return builder.build().toString();
    }

    static class Impl implements Visitor<CodeBlock.Builder> {
        @Override
        public void visitType(Node.@NotNull Type node, CodeBlock.Builder code) {
            code.add("declare class $L", node.name());
            if (node.superClass() != null) {
                code.add(" extends $L", ((ClassName) node.superClass()).simpleName().replace("Lua", ""));
            }

            code.add("$>\n");
            for (Node.Property property : node.properties()) {
                property.accept(this, code);
            }

            code.add("\n");

            for (Node.Method method : node.methods()) {
                method.accept(this, code);
            }

            code.add("$<end\n");
        }

        @Override
        public void visitProperty(Node.@NotNull Property node, CodeBlock.Builder code) {
            code.add("$L: $L\n", node.name(), node.type().luaType());
        }

        @Override
        public void visitMethodActual(Node.Method.@NotNull Actual node, CodeBlock.Builder code) {
            code.add("function $L(self", node.name());

            for (var arg : node.args()) {
                code.add(", $L: $L", arg.name(), arg.type().luaType());
            }
            code.add(")");

            if (node.ret() != null) {
                code.add(": $L", node.ret().luaType());
            }

            code.add("\n");
        }
    }


}
