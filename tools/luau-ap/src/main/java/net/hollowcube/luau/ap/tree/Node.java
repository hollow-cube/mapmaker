package net.hollowcube.luau.ap.tree;

import com.squareup.javapoet.TypeName;
import net.hollowcube.luau.ap.util.DocContent;
import net.hollowcube.luau.ap.util.LuaTypeMirror;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import java.util.List;

public interface Node {

    @NotNull Element elem();

    <P> void accept(Visitor<P> visitor, P param);

    record Type(
            @NotNull String name,
            @NotNull Element elem,

            @NotNull TypeName implClass,
            @NotNull TypeName wrapperClass,
            @Nullable TypeName superClass,
            @Nullable DocContent doc,

            @NotNull List<Property> properties,
            @NotNull List<Method> methods
            //todo metamethods
    ) implements Node {
        @Override
        public <P> void accept(Visitor<P> visitor, P param) {
            visitor.visitType(this, param);
        }
    }

    record Property(
            @NotNull Element elem,

            @NotNull String name,
            @NotNull String accessor, // How to fetch the property. Eg just the name for fields, or name() for methods.

            @NotNull LuaTypeMirror type,
            @Nullable DocContent doc
            //todo what is accessor for?
    ) implements Node {
        @Override
        public <P> void accept(Visitor<P> visitor, P param) {
            visitor.visitProperty(this, param);
        }
    }

    interface Method extends Node {
        @NotNull String name();

        record Arg(@NotNull String name, @NotNull LuaTypeMirror type) {
        }

        record Actual(
                @NotNull Element elem,

                @NotNull String name, // Lua Name
                @NotNull String methodName, // Java Name

                // If present, args and ret are ignored.
                boolean isDirect,
                // If present, there is an implicit first arg of the type being implemented, unless its direct.
                boolean isStatic,
                // Method defined on wrapper class
                boolean forceWrapper,

                @NotNull List<Arg> args,
                @Nullable LuaTypeMirror ret,
                @Nullable DocContent doc
        ) implements Method {
            @Override
            public <P> void accept(Visitor<P> visitor, P param) {
                visitor.visitMethodActual(this, param);
            }
        }

        record Overload(
                @NotNull Element elem,
                @NotNull String name,

                @NotNull List<Actual> overloads
        ) implements Method {
            @Override
            public <P> void accept(Visitor<P> visitor, P param) {
                visitor.visitMethodOverload(this, param);
            }
        }
    }
}
