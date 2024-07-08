package net.hollowcube.luau.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LuaMeta {

    @NotNull Type value();

    enum Type {
        // Special

        /**
         * Control 'prototype' inheritance. When accessing "myTable[key]" and the key does not appear in the table, but the metatable has an __index property:
         */
        INDEX,
        /**
         * Control property assignment. When calling "myTable[key] = value", if the metatable has a __newindex key pointing to a function, call that function, passing it the table, key, and value.
         */
        NEWINDEX,
        //__mode -> Control weak references. A string value with one or both of the characters 'k' and 'v' which specifies that the the keys and/or values in the table are weak references.
        /**
         * Treat a table like a function. When a table is followed by parenthesis such as "myTable( 'foo' )" and the metatable has a __call key pointing to a function, that function is invoked (passing the table as the first argument, followed by any specified arguments) and the return value is returned.
         */
        CALL,
        //__namecall -> Faster alternative to obj:Method(args). Only invoked on userdata and builtin types.
        /**
         * Control string representation. When the builtin "tostring( myTable )" function is called, if the metatable for myTable has a __tostring property set to a function, that function is invoked (passing myTable to it) and the return value is used as the string representation.
         */
        TOSTRING,
        //__len -> Control table length that is reported. When the table length is requested using the length operator ( '#' ), if the metatable for myTable has a __len key pointing to a function, that function is invoked (passing myTable to it) and the return value used as the value of "#myTable".
        //__iter -> todo
        //__type -> String field describing the type name of the object
        //__metatable -> Hide the metatable. When "getmetatable( myTable )" is called, if the metatable for myTable has a __metatable key, the value of that key is returned instead of the actual metatable.

        // Arithmetic

        //__unm -> Unary minus. When writing "-myTable", if the metatable has a __unm key pointing to a function, that function is invoked (passing the table), and the return value used as the value of "-myTable".
        /**
         * Addition. When writing "myTable + object" or "object + myTable", if myTable's metatable has an __add key pointing to a function, that function is invoked (passing the left and right operands in order) and the return value used.
         */
        ADD,
        /**
         * Subtraction. Invoked similar to addition, using the '-' operator.
         */
        SUB,
        //__mul -> Multiplication. Invoked similar to addition, using the '*' operator.
        //__div -> Division. Invoked similar to addition, using the '/' operator.
        //__idiv -> Floor division (division with rounding down to nearest integer). '//' operator.
        //__mod -> Modulo. Invoked similar to addition, using the '%' operator.
        //__pow -> Involution. Invoked similar to addition, using the '^' operator.
        //__concat -> Concatenation. Invoked similar to addition, using the '..' operator.

        // Equality

        /**
         * Check for equality. This method is invoked when "myTable1 == myTable2" is evaluated, but only if both tables have the exact same metamethod for __eq.
         */
        EQ,
        //__le -> Check for less-than. Similar to equality, using the '<' operator.
        //__lt -> Check for less-than-or-equal. Similar to equality, using the '<=' operator.
    }
}
