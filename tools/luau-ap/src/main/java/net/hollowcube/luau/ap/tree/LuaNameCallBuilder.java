package net.hollowcube.luau.ap.tree;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import net.hollowcube.luau.ap.Types;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;

public class LuaNameCallBuilder {

    public static @NotNull MethodSpec create(@NotNull Node.Type typeNode) {
        var builder = MethodSpec.methodBuilder("generatedLuaNameCall");
        builder.addModifiers(Modifier.STATIC);
        builder.returns(int.class);
        builder.addParameter(Types.LUA_STATE, "state");
        typeNode.accept(new Impl(), builder);
        return builder.build();
    }

    static class Impl implements Visitor<MethodSpec.Builder> {
        private boolean isOverload = false;

        @Override
        public void visitType(Node.@NotNull Type node, MethodSpec.Builder builder) {
            builder.addStatement("final $T ref = $T.checkUserDataArg(state, 1, $T.class)",
                    node.implClass(), Types.LUA_HELPERS, node.implClass());
            builder.addStatement("String methodName = state.nameCallAtom()");

            builder.addCode("return switch (methodName) {$>\n");
            Visitor.super.visitType(node, builder);

            // Append handler for the default case.
            builder.addCode("default -> {$>\n");
            if (node.superClass() instanceof ClassName superClass) {
                var superTypeWrapper = ClassName.get(superClass.packageName(), superClass.simpleName() + "$Wrapper");
                builder.addStatement("yield $T.generatedLuaNameCall(state)", superTypeWrapper);
            } else {
                builder.addStatement("state.error(\"No such method: \" + methodName)");
                builder.addStatement("yield 0");
            }
            builder.addCode("$<}\n");

            builder.addCode("$<};");

        }

        @Override
        public void visitMethodOverload(Node.Method.@NotNull Overload node, MethodSpec.Builder builder) {
            builder.addCode("case $S -> {$>\n", node.name()); // Common method header

            isOverload = true;
            builder.addStatement("int argCount = state.getTop() - 1");

            for (Node.Method.Actual actual : node.overloads()) {
                // Add signature check
                var checkBuilder = new StringBuilder();
                for (int i = 0; i < actual.args().size(); i++) {
                    var arg = actual.args().get(i);
                    checkBuilder.append(" && ");
                    checkBuilder.append(arg.type().createCheck(i + 2));
                }
                builder.beginControlFlow("if (argCount == $L$L)", actual.args().size(), checkBuilder);

                actual.accept(this, builder);

                builder.endControlFlow();
            }

            // In case we did not match, include generic error like "no matching call for myMethod(1, 2, 3)"
            builder.addStatement("var errorBuilder = new StringBuilder()");
            builder.addStatement("errorBuilder.append(\"No applicable overload for \" + methodName + \"(\")");
            builder.beginControlFlow("for (int i = 0; i < argCount; i++)");
            builder.beginControlFlow("if (i > 0)");
            builder.addStatement("errorBuilder.append(\", \")");
            builder.endControlFlow();
            builder.addStatement("errorBuilder.append(state.toString(i + 2))");
            builder.endControlFlow();
            builder.addStatement("errorBuilder.append(\")\")");
            builder.addStatement("state.error(errorBuilder.toString())");
            builder.addStatement("yield 0");
            isOverload = false;

            builder.addCode("$<}\n"); // Common method footer
        }

        @Override
        public void visitMethodActual(Node.Method.@NotNull Actual node, MethodSpec.Builder builder) {
            if (!isOverload) builder.addCode("case $S -> {$>\n", node.name()); // Common method header

            if (node.isDirect()) {
                builder.addStatement("yield ref.$N(state)", node.methodName());
            } else {
                for (int i = 0; i < node.args().size(); i++) {
                    var arg = node.args().get(i);
                    // Offset by 2 because we are 1 indexed and the first arg is the userdata object.
                    arg.type().insertPop(builder, arg.name(), i + 2);
                }

                if (node.ret() != null) {
                    builder.addCode("var result = ");
                }
                builder.addStatement("ref.$N($L)", node.methodName(), node.args().stream()
                        .map(Node.Method.Arg::name)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""));

                if (node.ret() != null) {
                    node.ret().insertPush(builder, "result");
                }

                // If the method has a return value, yield 1, otherwise yield 0.
                builder.addStatement("yield $L", node.ret() == null ? 0 : 1);
            }

            if (!isOverload) builder.addCode("$<}\n"); // Common method footer
        }
    }
}
